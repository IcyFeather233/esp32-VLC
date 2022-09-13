int iLEDID=4095;  //iLEDID 表示要传输的id数据，ID 最大是4095, 也就是二进制的 1111 1111 1111

#define LED 2     // 定义IO口

static unsigned char LEDID[2]={0,10};
// the LED's ID using low 6 bits
static unsigned int LEDMID[2];          // The LED's ID in Manchseter
static unsigned int LEDMIDT;            // LEDMIDT 目前传的包的数据， 
static unsigned char LEDIDcnt=0;        // the counter for transmission of LED'ID
static unsigned char LEDT_count=1;      // counting sending bits

#define IDbitsNO 2

long previousTime = 0;
long interval = 250; // 250ms -> 4k Hz

void setup() {
    Serial.begin(115200);
    pinMode(LED,OUTPUT);
    digitalWrite(LED,0);
    Init_IDTransmission(iLEDID);  
}

void loop() {
    unsigned long currentTime = millis();
    if (currentTime - previousTime > interval) {
        previousTime = currentTime;
        LEDTransmittingID(); 
    }
}

void Init_IDTransmission(unsigned int transID)
{
    // transId原来有12位，分成高六位和第六位
    unsigned char tID1, tID2;
    unsigned char IDH, IDL;

    // IDH 代表高六位，IDL代表第六位
    IDH=transID/64; 
    IDL=transID%64;
    Serial.println(IDH);
    Serial.println(IDL);
    // tID 表示加了序号位的id
    tID1=IDH&0x3F; // 3f是111111，把00放到高六位前面
    tID2=(IDL&0x3F)|0x40; // 40是1000000，把01放到低六位前面

    // LEDMID 表示曼彻斯特编码后的数据，经过曼彻斯特码后，原来的8位变成了16位
    LEDMID[0]=CharToManchester(tID1);
    LEDMID[1]=CharToManchester(tID2);
    Serial.println(LEDMID[0], HEX);
    Serial.println(LEDMID[1], HEX);
    // LEDMIDT 目前传的包的数据
    LEDMIDT=LEDMID[0];

}

void LEDTransmittingID(void)
{
    // LEDT_count 表示目前传的第几位
    if(LEDT_count<4) // 1-3 start bits 111
    {
         digitalWrite(LED, 1);
         Serial.print("1");
    }
    else if(LEDT_count<5) // the 4th bit of 0
    {
          digitalWrite(LED, 0);
          Serial.print("0 ");
    }
    else  
    {
        if(LEDT_count<21) // 5-20 data bits 16bits (manchester code of a byte) data
        {
            // 0x8000 -> 1后面接15个0
            if((LEDMIDT & 0x8000) == 0x8000)// send 1
            {
                 digitalWrite(LED,1);
                 Serial.print("1");
            }
            else   //send 0
            {
                  digitalWrite(LED,0);
                  Serial.print("0");
            }
            // 左移一位，也就是下次判断下一位
            LEDMIDT <<= 1;
        }
        else
        {
            Serial.print(" ");
            digitalWrite(LED,0);  // 1 stop  bits 0
            Serial.println("0");
            // 一个数据包传输完了，将LEDT_count 表示目前传的第几位归零
            LEDT_count=0;   // clear M_count

            // 传下一个数据包
            LEDIDcnt++;
            
            // 两个都传完了就重新传第一个包
            if (LEDIDcnt==IDbitsNO)
            {
                LEDIDcnt=0;
            }
            // LEDMIDT 目前传的包的数据更新
            LEDMIDT=LEDMID[LEDIDcnt];
           
        }
    }
    // LEDT_count 目前传到了第几位+1
    LEDT_count++; //bits of frame counting (total 20 bits) 1-20
}

// char数组（里面是01串）转换成曼彻斯特码
unsigned int CharToManchester(unsigned char DataIn)
{
    unsigned int ManchesterOut = 0;
    unsigned char i=0;
    for(i=0;i<7;i++)
    {
        if((DataIn & 0x80) == 0x80)//1->10
        {
            ManchesterOut += 1;//write 1
            ManchesterOut <<= 1;//move a bit (it's equal to write 0 into the output)
            ManchesterOut <<= 1;//move a bit for preparation of next coding
        }
        if((DataIn & 0x80) == 0)//0->01
        {
            ManchesterOut <<= 1;//move a bit (it's equal to write 0 into the output)
            ManchesterOut += 1;//write 1
            ManchesterOut <<= 1;//move a bit for preparation of next coding
        }
        DataIn <<= 1;
    }
    //code the eighth bit
    if((DataIn & 0x80) == 0x80)//1->10
    {
        ManchesterOut += 1;
        ManchesterOut <<= 1;
    }
    if((DataIn & 0x80) == 0)//0->01
    {
        ManchesterOut <<= 1;
        ManchesterOut |= 1;
    }
    return ManchesterOut;//return the manchester
}

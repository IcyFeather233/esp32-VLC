int iLEDID=4095;//ID 最大是4095  
#define LED 2
static unsigned char LEDID[2]={0,10}; //the LED's ID using low 6 bits
static unsigned int LEDMID[2];//The LED's ID in Manchseter
static unsigned int LEDMIDT, tLEDMID;
static unsigned char LEDIDcnt=0;  //the counter for transmission of LED'ID
//static unsigned char RetransCnt=0;
static unsigned char LEDT_count=1; // counting sending bits

#define IDbitsNO 2

void setup() {
  // put your setup code here, to run once:
  Serial.begin(115200);
  pinMode(LED,OUTPUT);
  digitalWrite(LED,0);
 //  delay(1000);
  Init_IDTransmission(iLEDID);  
}

void loop() {
  // put your main code here, to run repeatedly:
 LEDTransmittingID(); 
 delay(100);//每一个bit延时的时间
}

void Init_IDTransmission(unsigned int transID)
{

  unsigned char tID1,tID2;
  unsigned char  IDH,IDL;
    /***Read the ID from eeprom**/ 
    IDH=transID/64; 
    IDL=transID%64;
    Serial.println(IDH);
    Serial.println(IDL);
    tID1=IDH&0x3F; //Embedding PSN 00 into high 2 bit
    tID2=(IDL&0x3F)|0x40; //Embedding PSN 01 into high 2 bit
    
    LEDMID[0]=CharToManchester(tID1); //21865-0101 0101 0110 1001
    LEDMID[1]=CharToManchester(tID2); //21,865--
      Serial.println(LEDMID[0],HEX);
      Serial.println(LEDMID[1],HEX);
     LEDMIDT=LEDMID[0];
//     tLEDMID=LEDMID[0];
}

void LEDTransmittingID(void)
{
  if(LEDT_count<4) // 1-3 start bits 111
  {
     digitalWrite(LED,1);
     Serial.print("1");
  }
  else if(LEDT_count<5) // the 4th bit of 0
  {
      digitalWrite(LED,0);
      Serial.print("0 ");
  }
  else  
  {
      if(LEDT_count<21) // 5-20 data bits 16bits (manchester code of a byte) data
      {
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
         LEDMIDT <<= 1;
      }
      else
      {
        Serial.print(" ");
           digitalWrite(LED,0);  // 1 stop  bits 0
              Serial.println("0");
           LEDT_count=0;   // clear M_count
       //    OCCTransEND=1;  //Added by YYB 2021-3-18 to flag OCC transmission end
         //  LEDMIDT=tLEDMID;  //manchester coding results

         //  RetransCnt++;
         //  if(RetransCnt>RETRANS)  // after continous transimtting 3 times then update codes
           {
             //   RetransCnt=0;
               
 
                LEDIDcnt++;  //Added by YYB 2016-06-07 for testing
                     if (LEDIDcnt==IDbitsNO)
                {
                   LEDIDcnt=0;
                }
                 LEDMIDT=LEDMID[LEDIDcnt];  // Most significant 8 bits -PSN
           
           }
      }
  }
  LEDT_count++; //bits of frame counting (total 20 bits) 1-20
}

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

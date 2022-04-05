#include <Manchester.h>
#include <Arduino.h>
#include <stdio.h>

#define PIN_RLED D8
#define PIN_GLED D10
#define PIN_BLED D2

unsigned long lastChangeTime = 0;
unsigned long changeTime = 500;

unsigned char data = 'I';
unsigned int out = 0;
int cnt = 8;

int ledState = HIGH;

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

void setup()
{
    // 初始化串口
    Serial.begin(115200);

    // 初始化有LED的IO
    pinMode(PIN_RLED, OUTPUT);
    pinMode(PIN_GLED, OUTPUT);
    pinMode(PIN_BLED, OUTPUT);
    digitalWrite(PIN_RLED, HIGH);
    digitalWrite(PIN_GLED, HIGH);
    digitalWrite(PIN_BLED, HIGH);
    lastChangeTime = micros();

    out = CharToManchester(data);
}

void loop()
{
    if ((micros() - lastChangeTime) > changeTime) {
        ledState = out & (1 << cnt);
        lastChangeTime = micros();
        digitalWrite(PIN_RLED, ledState);
        digitalWrite(PIN_GLED, ledState);
        digitalWrite(PIN_BLED, ledState);
        cnt = (cnt - 1 + 8) % 8;
    }
}

/*
*Blinker控制RGB三色灯程序
*小功率三色灯珠共阴极接GND, 阳极极经限流电阻接IO口；IO口状态为高时，灯亮。
*根据LED共阴共阳，自己调整程序。
*大功率LED可以根据功率选择相应的放大器，如MOS管
*硬件ESP8266
*/
 
//重定义引脚
#define PIN_RLED D8
#define PIN_GLED D10
#define PIN_BLED D2

unsigned long lastChangeTime = 0;
unsigned long changeTime = 500;

int ledState = HIGH;
 
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
 
}
 
void loop() {
    if ((micros() - lastChangeTime) > changeTime) {
      ledState = !ledState;
      lastChangeTime = micros();
      digitalWrite(PIN_RLED, ledState);
      digitalWrite(PIN_GLED, ledState);
      digitalWrite(PIN_BLED, ledState);
    }
}

# esp32-VLC

Use esp32 and ws2812 to make a Visible Light Communication System. 

## PCB

[lceda esp12f VLC](https://u.lceda.cn/account/user/projects/index/detail?project=e002af75aaf0421280fabf62bafaa944&folder=all)

## drive ws2812

Drive an WS2812 LED strip based on the RMT driver [SalimTerryLi/pwe-virtual-driver](https://github.com/SalimTerryLi/pwe-virtual-driver)

## Manchester Coding

Here is [YYB's manchester-OOK](https://paste.ubuntu.com/p/fJGT6d4YjY/)

更新：上面的皮卡说不能用，上面是 stm32 的代码，用 stm32 的时钟实现的，arduino 不行，要手写 machester ook 编码

https://github.com/mchr3k/arduino-libs-manchester

## Android app

扫描解码部分已经完成，别的什么都没有

--------------

# connection

## 0.66'' 64x48 oled - esp8266MOD

| oled        | esp8266     |
| ----------- | ----------- |
| D1          | D1          |
| D2          | D2          |
| GND         | GND         |
| 3v3         | 3v3         |

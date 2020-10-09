# BLE_UART
作者由于嵌入式开发的需要，要将nrf52832的数据通过蓝牙传输到远程安卓设备\r\n
目前缺少合适的适用于BLE的串口调试助手，且串口调试助手使用起来并不方便
网上的教程分为官方库和私人开源库两种，官方库的问题在于需要手动实现对操作队列，否则会出现严重的时序问题，工作量大
由于本人是安卓开发新手，各位大牛的私人开源库混杂了诸多安卓界面操作等方面的API，使得我难以理解其示例代码，最后我找到了一个比较合适的，有教程的，适合初学者的库
如果你有一定的安卓开发经验，想要进行BLE开发，请参考百度上的官方库开发资料或者各种注释较少的开源库，可能更为方便
如果你也是安卓开发初学者，只是想要尽快实现安卓端的BLE（串口）通信，可以参考本示例的代码，在此鸣谢BleLib开源库的作者
BLELib: https://github.com/NoHarry/BLELib
BLElib作者教程: http://noharry.cc/2018/06/28/Blelib%E4%BD%BF%E7%94%A8%E8%AF%B4%E6%98%8E/
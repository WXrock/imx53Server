#define GPIO_FILE "/dev/bjw-gpio"
#define GPIO_GET_DBGCTRL   0x40044700
#define GPIO_SET_DBGCTRL   0x40044701
#define GPIO_GET_CFGPIN   0x40044702
#define GPIO_SET_CFGPIN   0x40044703
#define GPIO_GET_PULL  0x40044704
#define GPIO_SET_PULL  0x40044705

extern int fd_gpio;

extern int readGPIO(int pin_port, int pin_num);
extern int writeGPIO(int pin_port, int pin_num, int pin_val);

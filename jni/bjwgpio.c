#include <stdio.h>
#include <stdlib.h>
#include <android/log.h>
#include "bjwgpio.h"  
#include <string.h>

#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <jni.h>
#include <assert.h>

#include <android/log.h>

#define LOG_TAG "GPIO"
#define DEBUG
#ifdef DEBUG
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#else
#define LOGD(...)
#define LOGI(...)
#endif

int fd_err=-2;

struct gpio_info {
	char* pin_group;
	int pin_num;
};

struct gpio_pin_data {
	char* pin_group;
	int pin_num;
	int pin_data;
};



int readGPIO(int pin_port, int pin_num)
{
	 char cmd_buf[16];
	 //char *pin_char;
	 int val;
	 //pin_char=jstringTostr(env,pin_group);
	 sprintf(cmd_buf, "%d %d", pin_port,pin_num);
	 LOGD("fd_gpio is %d ,cmd is %s",fd_gpio,cmd_buf);
	 if(fd_gpio<0)
	 	return fd_err;
	 val = read(fd_gpio, cmd_buf, strlen(cmd_buf));
	 LOGD("ReadGPIO return is %d",val);
	 return val;
}

 int writeGPIO(int pin_port, int pin_num, int pin_val)
{
	 char cmd_buf[3];
	 int val;
	 sprintf(cmd_buf, "%d %d %d", pin_port,pin_num,pin_val);
	 LOGD("fd_gpio is %d ,cmd is %d",fd_gpio,cmd_buf);
	 if(fd_gpio<0)
	 	return fd_err;
	 val = write(fd_gpio, cmd_buf, strlen(cmd_buf));
	 LOGD("WriteGPIO return is %d",val);
	 return val;
}








/*
 * v4l2-bmp.c
 *
 *  Created on: 2014年8月6日
 *      Author: ljw
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>


#include <assert.h>
#include <getopt.h>
#include <fcntl.h>
#include <unistd.h>
#include <errno.h>
#include <malloc.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/time.h>
#include <sys/mman.h>
#include <sys/ioctl.h>

#include <asm/types.h>
#include <linux/videodev2.h>
#include <linux/mxc_v4l2.h>

#include<jni.h>
#include <time.h>
#include <android/log.h>

#include "jpeglib.h"
#include "bjwgpio.h"

#define CAMERA_DEVICE "/dev/video0"
#define SEND 26
#define CAMSEL 27
#define MODE 16

#define  LOG_TAG    "libpicture"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

#define CAPTURE_JPEG_FILE0 "/sdcard/frame_jpeg_new0.jpg"
#define CAPTURE_JPEG_FILE1 "/sdcard/frame_jpeg_new1.jpg"
#define CAPTURE_JPEG_FILE2 "/sdcard/frame_jpeg_new2.jpg"
#define CAPTURE_JPEG_FILE3 "/sdcard/frame_jpeg_new3.jpg"
#define CAPTURE_JPEG_FILE4 "/sdcard/frame_jpeg_new4.jpg"
#define CAPTURE_JPEG_FILE5 "/sdcard/frame_jpeg_new5.jpg"
#define CAPTURE_JPEG_FILE6 "/sdcard/frame_jpeg_new6.jpg"
#define CAPTURE_JPEG_FILE7 "/sdcard/frame_jpeg_new7.jpg"

#define VIDEO_WIDTH 2048
#define VIDEO_HEIGHT 1536
#define VIDEO_FORMAT V4L2_PIX_FMT_YUV420
#define BUFFER_COUNT 3   //3
#define DEFAULT_FPS 15
#define BUF_SIZE 4718592  //2048*1536*1.5
#define NUM 8  //the num of camera

#define CLEAR(x) memset(&(x),0,sizeof(x))



/* Return current time in milliseconds */
static void print_time(void)
{
    struct timeval tv;
    gettimeofday(&tv, NULL);
    LOGI("TIME IS:%f\n",tv.tv_sec*1000. + tv.tv_usec/1000.);
}

typedef struct VideoBuffer {
    void   *start; //视频缓冲区的起始地址
    size_t  length;//缓冲区的长度
} VideoBuffer;


VideoBuffer framebuf[BUFFER_COUNT];   //修改了错误，2012-5.21
int fd;
struct v4l2_capability cap;
struct v4l2_fmtdesc fmtdesc;
struct v4l2_format fmt;
struct v4l2_streamparm parm;
struct v4l2_requestbuffers reqbuf;
struct v4l2_buffer buf;
struct v4l2_control control;
unsigned char *starter;
unsigned char *newBuf;
int fd_gpio;
unsigned char *tmp_buf[8];


static int open_device()
{

	int fd;
    fd = open(CAMERA_DEVICE, O_RDWR, 0);//
    if (fd < 0) {
        LOGI("Open %s failed\n", CAMERA_DEVICE);
        return -1;
    }
	return fd;
}


static void set_input() {
    int index = 1;
    if( ioctl(fd,VIDIOC_S_INPUT,&index) != 0) {
        LOGI("S_INPUT FAIL\n");
    }
    return;
}


static void get_capability()
{// 获取驱动信息
    int ret = ioctl(fd, VIDIOC_QUERYCAP, &cap);
	if (ret < 0) {
        LOGI("VIDIOC_QUERYCAP failed (%d)\n", ret);
        return;
    }
    // Print capability infomations
    LOGI("------------VIDIOC_QUERYCAP-----------\n");
    LOGI("Capability Informations:\n");
    LOGI(" driver: %s\n", cap.driver);
    LOGI(" card: %s\n", cap.card);
    LOGI(" bus_info: %s\n", cap.bus_info);
    LOGI(" version: %08X\n", cap.version);
    LOGI(" capabilities: %08X\n\n", cap.capabilities);
	return;
}


static void get_format()
{
	int ret;
	fmtdesc.index=0;
    fmtdesc.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
	ret=ioctl(fd, VIDIOC_ENUM_FMT, &fmtdesc);
	while (ret != 0)
    {
        fmtdesc.index++;
		ret=ioctl(fd, VIDIOC_ENUM_FMT, &fmtdesc);
    }
	LOGI("--------VIDIOC_ENUM_FMT---------\n");
    LOGI("get the format what the device support\n{ pixelformat = ''%c%c%c%c'', description = ''%s'' }\n"
        ,fmtdesc.pixelformat & 0xFF, (fmtdesc.pixelformat >> 8) & 0xFF, (fmtdesc.pixelformat >> 16) & 0xFF,(fmtdesc.pixelformat >> 24) & 0xFF, fmtdesc.description);

	return;
}

static void setControl() {
    control.id = V4L2_CID_MXC_VF_ROT;
    control.value = V4L2_MXC_CAM_ROTATE_VERT_FLIP;
    int ret = ioctl(fd,VIDIOC_S_CTRL,&control);
    if(ret <0) {
        LOGI("flip failed (%d)\n", ret);
    }else{
        LOGI("flip set (%d)\n", ret);
    }
}

static int set_format()
{
	fmt.type                = V4L2_BUF_TYPE_VIDEO_CAPTURE;
    fmt.fmt.pix.width       = VIDEO_WIDTH;
    fmt.fmt.pix.height      = VIDEO_HEIGHT;
    fmt.fmt.pix.pixelformat = VIDEO_FORMAT;//fmtdesc.pixelformat;
    //fmt.fmt.pix.field       = V4L2_FIELD_INTERLACED;
    int ret = ioctl(fd, VIDIOC_S_FMT, &fmt);
	if (ret < 0) {
        LOGI("VIDIOC_S_FMT failed (%d)\n", ret);
        return ret;
    }

    // Print Stream Format
	LOGI("------------VIDIOC_S_FMT---------------\n");
    LOGI("Stream Format Informations:\n");
    LOGI(" type: %d\n", fmt.type);
    LOGI(" width: %d\n", fmt.fmt.pix.width);
    LOGI(" height: %d\n", fmt.fmt.pix.height);


    char fmtstr[8];
    memset(fmtstr, 0, 8);

    memcpy(fmtstr, &fmt.fmt.pix.pixelformat, 4);
    LOGI(" pixelformat: %s\n", fmtstr);
    LOGI(" field: %d\n", fmt.fmt.pix.field);
    LOGI(" bytesperline: %d\n", fmt.fmt.pix.bytesperline);
    LOGI(" sizeimage: %d\n", fmt.fmt.pix.sizeimage);
    LOGI(" colorspace: %d\n", fmt.fmt.pix.colorspace);
    LOGI(" priv: %d\n", fmt.fmt.pix.priv);
    LOGI(" raw_date: %s\n", fmt.fmt.raw_data);
	return 0;
}

static int set_parm() {

  int ret = 0;
  //set parm
  parm.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
  parm.parm.capture.timeperframe.denominator = DEFAULT_FPS;
  parm.parm.capture.timeperframe.numerator = 1;
  parm.parm.capture.capturemode = 3; //mode:2048_1536
  ret = ioctl(fd,VIDIOC_S_PARM,&parm);
  if (ret < 0) {
        LOGI("VIDIOC_S_PARM failed (%d)\n", ret);
        return ret;
  }
  LOGI("TK-------->>>>>>set parm\n");
  return ret;

}

static int request_buf()
{
    int ret = 0;

	reqbuf.count = BUFFER_COUNT;
    reqbuf.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
    reqbuf.memory = V4L2_MEMORY_MMAP;

    ret = ioctl(fd , VIDIOC_REQBUFS, &reqbuf);
    if(ret < 0) {
        LOGI("VIDIOC_REQBUFS failed (%d)\n", ret);
        return ret;
    }
	LOGI("the buffer has been assigned successfully!buffer size:%d\n",ret);
	return ret;
}


static void query_map_qbuf()
{
	int i,ret;
    for (i = 0; i < reqbuf.count; i++)
    {
        buf.index = i;
        buf.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
        buf.memory = V4L2_MEMORY_MMAP;
        ret = ioctl(fd , VIDIOC_QUERYBUF, &buf);//buf取得内存缓冲区的信息
        if(ret < 0) {
            LOGI("VIDIOC_QUERYBUF (%d) failed (%d)\n", i, ret);
            return;
        }


        // mmap buffer
        framebuf[i].length = buf.length;//framebuf是程序最前面定义的一个结构体类型的数据
        framebuf[i].start = (char *) mmap(0, buf.length, PROT_READ|PROT_WRITE, MAP_SHARED, fd, buf.m.offset);
        if (framebuf[i].start == MAP_FAILED) {
            LOGI("mmap (%d) failed: %s\n", i, strerror(errno));
            return;
        }

        ret = ioctl(fd , VIDIOC_QBUF, &buf);
        if (ret < 0) {
            LOGI("VIDIOC_QBUF (%d) failed (%d)\n", i, ret);
            return;
        }


        LOGI("Frame buffer %d: address=0x%x, length=%d\n", i, (unsigned int)framebuf[i].start, framebuf[i].length);
    }//空的视频缓冲区都已经在视频缓冲的输入队列中了
	return;
}


static int yuv420_to_jpeg(unsigned char *yuvData, int image_width, int image_height,
FILE *fp, int quality)
{
	  	 struct jpeg_compress_struct cinfo;
	     struct jpeg_error_mgr jerr;
	     JSAMPROW row_pointer[1];  // pointer to JSAMPLE row[s]
	     int row_stride;    // physical row width in image buffer
	     JSAMPIMAGE  buffer;

	     unsigned char *pSrc,*pDst;

	     int band,i,counter,buf_width[3],buf_height[3];
	     cinfo.err = jpeg_std_error(&jerr);

	     jpeg_create_compress(&cinfo);


	     jpeg_stdio_dest(&cinfo, fp);


	     cinfo.image_width = image_width;  // image width and height, in pixels
	     cinfo.image_height = image_height;
	     cinfo.input_components = 3;    // # of color components per pixel
	     cinfo.in_color_space = JCS_RGB;  //colorspace of input image

	     jpeg_set_defaults(&cinfo);

	     jpeg_set_quality(&cinfo, quality, TRUE );

	     //////////////////////////////
	     cinfo.raw_data_in = TRUE;
	     cinfo.jpeg_color_space = JCS_YCbCr;
	     cinfo.comp_info[0].h_samp_factor = 2;
	     cinfo.comp_info[0].v_samp_factor = 2;
	     /////////////////////////

	     jpeg_start_compress(&cinfo, TRUE);

	     buffer = (JSAMPIMAGE) (*cinfo.mem->alloc_small) ((j_common_ptr) &cinfo,
	                                 JPOOL_IMAGE, 3 * sizeof(JSAMPARRAY));
	     for(band=0; band <3; band++)
	     {
	         buf_width[band] = cinfo.comp_info[band].width_in_blocks * DCTSIZE;
	         buf_height[band] = cinfo.comp_info[band].v_samp_factor * DCTSIZE;
	         buffer[band] = (*cinfo.mem->alloc_sarray) ((j_common_ptr) &cinfo,
	                                 JPOOL_IMAGE, buf_width[band], buf_height[band]);
	     }

	     unsigned char *rawData[3];
	     rawData[0]=yuvData;
	     rawData[1]=yuvData+image_width*image_height;
	     rawData[2]=yuvData+image_width*image_height*5/4;

	     int src_width[3],src_height[3];
	     for(i=0;i<3;i++)
	     {
	         src_width[i]=(i==0)?image_width:image_width/2;
	         src_height[i]=(i==0)?image_height:image_height/2;
	     }

	     //max_lineÒ»°ãÎª16£¬ÍâÑ­»·Ã¿ŽÎŽŠÀí16ÐÐÊýŸÝ¡£
	     int max_line = cinfo.max_v_samp_factor*DCTSIZE;
	     for(counter=0; cinfo.next_scanline < cinfo.image_height; counter++)
	     {
	         //buffer image copy.
	         for(band=0; band <3; band++)  //Ã¿žö·ÖÁ¿·Ö±ðŽŠÀí
	         {
	              int mem_size = src_width[band];//buf_width[band];
	              pDst = (unsigned char *) buffer[band][0];
	              pSrc = (unsigned char *) rawData[band] + counter*buf_height[band] * src_width[band];//buf_width[band];  //yuv.data[band]·Ö±ð±íÊŸYUVÆðÊŒµØÖ·

	              for(i=0; i <buf_height[band]; i++)  //ŽŠÀíÃ¿ÐÐÊýŸÝ
	              {
	                   memcpy(pDst, pSrc, mem_size);
	                   pSrc += src_width[band];//buf_width[band];
	                   pDst += buf_width[band];
	              }
	         }
	         jpeg_write_raw_data(&cinfo, buffer, max_line);
	     }


	     jpeg_finish_compress(&cinfo);

	     jpeg_destroy_compress(&cinfo);

	     return 0;

}
void setMode(int val)
{
	fd_gpio = open(GPIO_FILE, O_RDWR, S_IRUSR | S_IWUSR);
	writeGPIO(3,MODE,val);
	close(fd_gpio);

}

void startCapture()
{
	writeGPIO(3,SEND,0);
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved)
{
	LOGI("JNI_OnLoad startup!");
	return JNI_VERSION_1_4;
}

JNIEXPORT void JNICALL JNI_Unload(JavaVM* vm, void* reserved)
{
	LOGI("JNI_UnLoad startup!");
	LOGI("fd_gpio is closed");
}

JNIEXPORT jint JNICALL Java_com_wx_imx53server_JniCamera_camSel(JNIEnv* env,
										 jobject thiz)
{
	jint ret = 0;

	fd_gpio = open(GPIO_FILE, O_RDWR, S_IRUSR | S_IWUSR);
	LOGI("fd_gpio is %d",fd_gpio);
	ret = writeGPIO(3,CAMSEL,1);
	usleep(1000);  //1ms
	ret = writeGPIO(3,CAMSEL,0);
	close(fd_gpio);

	return ret;
}

JNIEXPORT jint JNICALL Java_com_wx_imx53server_JniCamera_setMode(JNIEnv* env,
										 jobject thiz ,jint val)
{
	int i = val;
	setMode(i);
	return 0;
}

JNIEXPORT jint JNICALL Java_com_wx_imx53server_JniCamera_setFlip(JNIEnv* env,
                                        jobject thiz)
{
    fd = open_device();
    setControl();
    close(fd);
    return 0;
}

JNIEXPORT jint JNICALL Java_com_wx_imx53server_JniCamera_prepareBuffer(JNIEnv* env,
		 jobject thiz)
{
	jint ret = 0;
    int i;
	// 打开设备
	fd=open_device();

	// 获取驱动信息
	//struct v4l2_capability cap;
	get_capability();
	set_input();

	//获取当前视频设备支持的视频格式
	//struct v4l2_fmtdesc fmtdesc;
	memset(&fmtdesc,0,sizeof(fmtdesc));
	get_format();

	//set parm
	memset(&parm,0,sizeof(parm));
	set_parm();

	// 设置视频格式
	memset(&fmt, 0, sizeof(fmt));//将fmt中的前sizeof(fmt)字节用0替换并返回fmt
	set_format();

	// 请求分配内存
	ret = request_buf();
	if(ret < 0)
		return ret;

	// 获取空间，并将其映射到用户空间，然后投放到视频输入队列
	query_map_qbuf();
    
    //prepare for 8 buffers
    LOGI("prepare for 8 buffer");
    print_time();
    for(i=0;i<NUM;i++) {
        tmp_buf[i] = (unsigned char *)malloc(BUF_SIZE);
        if(tmp_buf[i]<0) {
            ret = -1;
            LOGI("MALLOC BUFFER FAILED!!!");
            return ret;
        }
    }
    print_time();
	//wait for stable
    usleep(500000); //wait 500 ms


	return ret;
}

JNIEXPORT jint JNICALL Java_com_wx_imx53server_JniCamera_takePicture(JNIEnv* env,
										 jobject thiz)
{
    int i,j;
    jint ret = 0;
    struct timeval tv;

	FILE *fp0 = fopen(CAPTURE_JPEG_FILE0, "wb");
    if (fp0 < 0) {
        LOGI("open frame data file failed\n");
        return -1;
    }
	FILE *fp1 = fopen(CAPTURE_JPEG_FILE1, "wb");
    if (fp1 < 0) {
        LOGI("open frame data file failed\n");
        return -1;
    }
	FILE *fp2 = fopen(CAPTURE_JPEG_FILE2, "wb");
    if (fp2 < 0) {
        LOGI("open frame data file failed\n");
        return -1;
    }
	FILE *fp3 = fopen(CAPTURE_JPEG_FILE3, "wb");
    if (fp3 < 0) {
        LOGI("open frame data file failed\n");
        return -1;
    }
	FILE *fp4 = fopen(CAPTURE_JPEG_FILE4, "wb");
    if (fp4 < 0) {
        LOGI("open frame data file failed\n");
        return -1;
    }
	FILE *fp5 = fopen(CAPTURE_JPEG_FILE5, "wb");
    if (fp5 < 0) {
        LOGI("open frame data file failed\n");
        return -1;
    }
	FILE *fp6 = fopen(CAPTURE_JPEG_FILE6, "wb");
    if (fp6 < 0) {
        LOGI("open frame data file failed\n");
        return -1;
    }
	FILE *fp7 = fopen(CAPTURE_JPEG_FILE7, "wb");
    if (fp7 < 0) {
        LOGI("open frame data file failed\n");
        return -1;
    }

    FILE* fp[8] = {fp1,fp2,fp3,fp4,fp5,fp6,fp7,fp0};

    // 开始录制
    enum v4l2_buf_type type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
    ret = ioctl(fd, VIDIOC_STREAMON, &type);
    if (ret < 0) {
        LOGI("VIDIOC_STREAMON failed (%d)\n", ret);
        return ret;
    }

    //write SEND gpio
    writeGPIO(3,SEND,1);

    //read one frame
    LOGI("read frame\n");
    for(i=0;i<NUM;i++){
    	print_time();
    	ret = ioctl(fd, VIDIOC_DQBUF, &buf);//VIDIOC_DQBUF命令结果, 使从队列删除的缓冲帧信息传给了此buf
    	if (ret < 0) {
    	    LOGI("VIDIOC_DQBUF failed (%d)\n", ret);
    	    return ret;
    	 }
    	if( memcpy(tmp_buf[i],framebuf[buf.index].start,framebuf[buf.index].length) < 0) {
            ret < 0;
            LOGI("MEMCPY FAILED !!!");
            return ret;
        }
   //     yuv420_to_jpeg(starter,VIDEO_WIDTH,VIDEO_HEIGHT,fp[i],100);
    	ret = ioctl(fd, VIDIOC_QBUF, &buf);
    	if (ret < 0) {
    	    LOGI("VIDIOC_QBUF failed (%d)\n", ret);
    	    return ret;
    	}
    	print_time();

    }

    for(i=0;i<NUM;i++) {     
        yuv420_to_jpeg(tmp_buf[i],VIDEO_WIDTH,VIDEO_HEIGHT,fp[i],100);
    }

	writeGPIO(3,SEND,0);

    // Release the resource
    for (i=0; i< BUFFER_COUNT; i++)
    {
        munmap(framebuf[i].start, framebuf[i].length);
    }

    if(-1 == ioctl(fd,VIDIOC_STREAMOFF,&type))
        LOGI("stream off error\n");

    close(fd);
    fclose(fp0);
    fclose(fp1);
    fclose(fp2);
    fclose(fp3);
    fclose(fp4);
    fclose(fp5);
    fclose(fp6);
    fclose(fp7);
    
    for(i=0;i<NUM;i++) {
        free(tmp_buf[i]);
    }
    
    writeGPIO(3,MODE,0);
    LOGI("Take Picture Done.\n");

    return ret;
}

#include "com_example_frank_main_MyNative.h"
#include "stdio.h"
#include "stdlib.h"
#include "string.h"
//导入日志头文件
#include <android/log.h>
//修改日志tag中的值
#define LOG_TAG "logfromc"
//日志显示的等级
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
/*
#include "stdafx.h"
*/

typedef unsigned char	U8;
typedef unsigned short	U16;
typedef unsigned int	U32;

#define FILE_NUM_MAX				(20)/* 最多20个image */

#define R11_IMAGE_ID_STR			("HYCO R11 IMAGE")
#define R11_IMAGE_ID_STR_LEN		(14)

#define R11_IMAGE_SIZE_MIN			(20*1024)
//#define R11_IMAGE_SIZE_MAX			(64*1024)
#define R11_IMAGE_SIZE_MAX			(126*1024)
typedef struct _r11_updateFile_header_
{
	char idStr[16];			//"HYCO R11 IMAGE"
	char versionStr[16];	//"R11 2.2.002"
	int  hwInfo;
	int  imageSize;
	int  imageOffset;
	int  crc;
} tR11_UPDATEFILE_HEADER;

typedef struct _r11_update
{
	tR11_UPDATEFILE_HEADER header;
	char data[R11_IMAGE_SIZE_MAX];
} tR11_UPDATE;

tR11_UPDATE r11_updateImage[FILE_NUM_MAX];
int validImageNum;

int update_flag = 1;

tR11_UPDATEFILE_HEADER *pHeader_final;
/*
	解析文件，输出文件名
	返回：-1---打开错误，-2---文件不合法
		>0----文件解析成功
*/
JNIEXPORT jint JNICALL Java_com_example_frank_main_MyNative_update_1fileParse
		(JNIEnv *env, jobject, jbyteArray file_name) {

	char *fileName = (char *) env->GetByteArrayElements(file_name, 0);

	FILE *fp;
	char buf[1024], *buf_temp, tempBuf[100];
	int versionStr_flag;
	int fileNum, fileSize, imageOffset;
	int i, j;
	char *pTempStr;
	unsigned long crc;
	tR11_UPDATEFILE_HEADER imageHeader[FILE_NUM_MAX];
	tR11_UPDATEFILE_HEADER *pHeader;
	U8 headerBuf[1024];

	fp = fopen(fileName, "rb");
	if (fp == NULL) {
		/* 文件打开错误 */
		return -1;
	}
	fread(&headerBuf[0], 1024, 1, fp);
//	for (int i = 0; i < 32; i++)
//	{
//		LOGI("string %X", headerBuf[i], 1024);//去字符串s%
//	}
	//fread(&imageHeader[0], FILE_NUM_MAX*sizeof(tR11_UPDATEFILE_HEADER), 1, fp);
	{
		/* 信息头AES解密 */
#include "aes.h"
		extern int AESDecode (BYTE *pUserKey, int keyLen, const char* srcString, int srcLen, char** dstString, int* dstLen);
#define USER_KEY_LEN	16
		const BYTE userKey[USER_KEY_LEN]=
				{
						0xEA, 0x45, 0x11, 0xEB, 0x02, 0xCE, 0x56, 0xAE,
						0xDE, 0x52, 0xEE, 0x42, 0xFC, 0x32, 0x7D, 0xCA
				};
		int ret;
		char *pcDecodeDst = NULL;
		int dstLen = 0;
		ret = AESDecode((BYTE *)userKey, USER_KEY_LEN, (const char *)headerBuf, 1024, &pcDecodeDst, &dstLen);
		if (ret == 0)
		{
			memcpy(&imageHeader[0], pcDecodeDst, FILE_NUM_MAX*sizeof(tR11_UPDATEFILE_HEADER));
		}
//		for (int i = 0; i < 48; i++)
//		{
//			LOGI("string %X",(jbyte) pcDecodeDst[i]);//去字符串s%
//		}
		free(pcDecodeDst);
	}

	fileNum = 0;
	for (i=0; i<FILE_NUM_MAX; i++)
	{
		pHeader = &imageHeader[i];
		/* 1. 判断标识 */
//		for (int j = 0; j < 16; j++)
//		{
//			LOGI("string %X", pHeader->idStr[j]);//去字符串s%
//		}
		int result = memcmp(& pHeader->idStr[0], R11_IMAGE_ID_STR, R11_IMAGE_ID_STR_LEN);
		if ( result != 0)
		{
			/* 标识错误，不再继续查找 */
//			for (int j = 0; j < 16; j++)
//			{
//				LOGI("string %X", pHeader->idStr[j]);//去字符串s%
//			}
//			for (int j = 0; j < 16; j++)
//			{
//				LOGI("string %X", imageHeader[i].idStr[j]);//去字符串s%
//			}
			LOGI("标识错误，不再继续查找 ,返回存在的头文件数量");
			break;
		}

		/* 2. 判断版本号字符 */
		versionStr_flag = 0;
		pTempStr = &pHeader->versionStr[0];
		for (j=0; j<9; j++)
		{
			if (pTempStr[j] != '.' &&
				(pTempStr[j] < '0' || pTempStr[j] > '9'))
			{
				/* 版本号字符错误 */
				LOGI("版本号字符错误 ");
				versionStr_flag = -1;
				break;
			}
		}
		if (versionStr_flag == -1)
		{
			/* 版本号字符错误，不再继续查找 */
			LOGI("版本号字符错误，不再继续查找 ");
			break;
		}
		/* 3. 判断硬件信息 */
		if (pHeader->hwInfo == 0)
		{
			/* 硬件信息错误 */
			LOGI("硬件信息错误 ");
			break;
		}

		/* 4. 判断升级数据大小 */
		if ( (pHeader->imageSize < R11_IMAGE_SIZE_MIN) || (pHeader->imageSize > R11_IMAGE_SIZE_MAX) )
		{
			/* 升级数据大小错误，不再继续查找 */
			LOGI("升级数据大小错误，不再继续查找 ");
			break;
		}

		memcpy(&r11_updateImage[fileNum].header, pHeader, sizeof(tR11_UPDATEFILE_HEADER));
		buf_temp = &r11_updateImage[fileNum].data[0];
		fseek(fp, pHeader->imageOffset, SEEK_SET);
		fread(buf_temp, pHeader->imageSize, 1, fp);

		fileNum++;
	}
	fclose(fp);

	if (fileNum == 0)
	{
		/* 没有找到合法的image信息，文件错误 */
		LOGI("没有找到合法的image信息，文件错误 ");
		return -2;
	}

	validImageNum = fileNum;
	return fileNum;
}

JNIEXPORT jint JNICALL Java_com_example_frank_main_MyNative_update_1checkSetFlag
        (JNIEnv *, jobject, jint flag)
{
	int ret = 1;

	if (flag != update_flag)
	{
		/* 需要重新发起升级请求 */
		ret = 0;
	}
	update_flag = flag;

	return ret;
}

char imageData[R11_IMAGE_SIZE_MAX];

/*
	获取指定index升级包的信息
	返回：-1/-2---参数超出有效image范围
		>0---------读取成功
*/
//int update_getImageInfo(int index, char **ppVerStr, int *pHwInfo,
//						int *pImageSize, int *pCrc, char **ppData)

JNIEXPORT jint JNICALL Java_com_example_frank_main_MyNative_update_1getImageInfo
        (JNIEnv *env, jobject obj, jint index, jbyteArray ppVer_Str, jbyteArray pHw_Info,
		 						jbyteArray pImage_Size, jbyteArray p_Crc, jbyteArray pp_Data)
{
    char *ppVerStr = (char*)env->GetByteArrayElements(ppVer_Str, 0); //传值为NULL，不能获取信息，否则崩溃
	char *pHwInfo = (char *) env->GetByteArrayElements(pHw_Info, 0);
	char *pImageSize = (char *) env->GetByteArrayElements(pImage_Size, 0);
	char *pCrc = (char *) env->GetByteArrayElements(p_Crc, 0);

	char * ppData = (char *)env->GetByteArrayElements(pp_Data,0);

	jclass class_Basic = (env)->FindClass("com/example/frank/main/MyNative");
	jfieldID fd = (env)->GetFieldID(class_Basic,"imageIndex","I");
	env->SetIntField(obj,fd,123);//注释（3）

//	class_Basic = (env)->FindClass("com/example/android/bluetoothlegatt/MyNative");
//	fd = (env)->GetFieldID(class_Basic,"imageIndex","I");
	//env->SetByteArrayRegion(pHw_Info, 0 , 4,(jbyte *)r11_updateImage[index].header.hwInfo);
	if (index > FILE_NUM_MAX)
	{
		/* 参数错误 */
		LOGI("参数错误");
		return -1;
	}
	if (index > validImageNum)
	{
		/* 该index没有有效数据 */
		LOGI("该index没有有效数据");
		return -2;
	}

	if (update_flag == 2)
	{
		/* 获取指定信息 */
		LOGI("升级标志0，清空所有数据");
		tR11_UPDATEFILE_HEADER *pHeader;
		char empty[4]={0};
		char *data;

		if (ppVerStr != NULL)
		{
			//LOGI("指定信息ppVerStr");
			//ppVerStr = &pHeader->versionStr[0];
			memset(ppVerStr,0,16);
		}
		if (pHwInfo != NULL)
		{
			//LOGI("指定信息pHwInfo");
			memcpy(pHwInfo, empty,4);//pHeader->hwInfo;
		}
		if (pImageSize != NULL)
		{
			//LOGI("指定信息pImageSize");
			memcpy(pImageSize, empty,4); //pHeader->imageSize;
		}
		if (pCrc != NULL)
		{
			//LOGI("指定信息pCrc");
			memcpy(pCrc, &empty,4);//pHeader->crc;
		}
		if (ppData != NULL)
		{
			//LOGI("指定信息ppData");
			memset(ppData,0,126*1024);
//			int i;char tempData;
//			for (i=0; i<126*1024; i++)
//			{
//				tempData = 0;
//				tempData = (data[i]>>4)&0x0F;//H-->L
//				tempData |= (data[i]<<4)&0xF0;//L-->H
//				imageData[i] = tempData;
//			}
//			ppData = imageData;
		}

		return 0;
	}

	LOGI("升级标志1，获取指定信息成功");
	if (ppVerStr != NULL)
	{
		LOGI("0，ppVerStr");
		//ppVerStr = &r11_updateImage[index].header.versionStr[0];
		memcpy(ppVerStr, r11_updateImage[index].header.versionStr, 16);
		for(int k=0;k<4;k++)
		{
			LOGI("string %X",(jbyte) ppVerStr[k]);//去字符串s%
		}
	}
	if (pHwInfo != NULL)
	{
		LOGI("1，pHwInfo");
		memcpy(pHwInfo, (char*)&r11_updateImage[index].header.hwInfo, 4);
	}
	if (pImageSize != NULL)
	{
		LOGI("2，pImageSize");
		memcpy(pImageSize,(char*)&r11_updateImage[index].header.imageSize, 4);
	}
	if (pCrc != NULL)
	{
		LOGI("3，pCrc");
		memcpy(pCrc, (char*)& r11_updateImage[index].header.crc, 4);
	}
	if (ppData != NULL)
	{
		LOGI("4，ppData");
		//ppData = r11_updateImage[index].data;
		memcpy(ppData, r11_updateImage[index].data, sizeof(r11_updateImage[index].data));
	}
	env->ReleaseByteArrayElements(ppVer_Str, (jbyte*)ppVerStr, 0);
	env->ReleaseByteArrayElements(pHw_Info, (jbyte*)pHwInfo, 0);
	env->ReleaseByteArrayElements(pImage_Size, (jbyte*)pImageSize, 0);
	env->ReleaseByteArrayElements(p_Crc, (jbyte*)pCrc, 0);
	return 0;
}

//调用java中的add方法 , 传递两个参数 jint x,y
JNIEXPORT void JNICALL Java_com_example_android_bluetoothlegatt_MyNative_headCallback
		(JNIEnv *env, jobject obj){
	char* classname = "com/example/android/bluetoothlegatt/MyNative";
//	jclass dpclazz = (*env)->FindClass(env,classname);
//	jmethodID methodID = (*env)->GetMethodID(env,dpclazz,"Add","(II)I");
//	(*env)->CallIntMethod(env, obj,methodID,3l,4l);
}

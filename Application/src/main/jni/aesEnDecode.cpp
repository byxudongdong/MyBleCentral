/*************** Header files *********************************************/
#include "aes.h"

//#define AES_DEBUG


int AESEncode (BYTE *pUserKey, int keyLen, const char* srcString, int srcLen, char** dstString, int* dstLen)
{
	//16 * (trunc(string_length / 16) + 1)¡£
	char *pOut=0;
	unsigned int len = 16 * (srcLen/16 + 1);

	BYTE	UserKey[AES_USER_KEY_LEN]={0};
	BYTE	IV[AES_BLOCK_LEN]={0};

	DWORD	UKLen, IVLen, SrcLen, DstLen;
	RET_VAL	ret;
	AES_ALG_INFO	AlgInfo;
	int eelen = 0;

	SrcLen = srcLen;
	UKLen = 16;
	IVLen = 16;
#ifdef AES_DEBUG
{
	int t = 0x10;
	int i;
	for (i=0; i<16; i++)
	{
		UserKey[i] = t+i;
	}
}
#else
	if (keyLen > 16) keyLen = 16;
	memcpy(UserKey, pUserKey, keyLen);
#endif

	pOut = (char*)malloc(len+4);
	if (pOut == NULL)	
		return -1;
	DstLen = len;

	//
	AES_SetAlgInfo(AES_ModeType, AES_PadType, IV, &AlgInfo);

	//	Encryption
	ret = AES_EncKeySchedule(UserKey, UKLen, &AlgInfo);
	if( ret!=CTR_SUCCESS )	
	{
		//writelog(LOG_DEBUG, "AES_EncKeySchedule() returns.");
		free (pOut);
		return -1;
	}
	ret = AES_EncInit(&AlgInfo);
	if( ret!=CTR_SUCCESS )	
	{
		//writelog(LOG_DEBUG, "AES_EncInit() returns.");
		free (pOut);
		return -1;
	}

	ret = AES_EncUpdate(&AlgInfo, (unsigned char*)srcString, SrcLen, (unsigned char*)pOut, &DstLen);
	if( ret!=CTR_SUCCESS )	
	{
		//writelog(LOG_DEBUG, "AES_EncUpdate() returns.");
		free (pOut);
		return -1;
	}

	eelen = DstLen;

	ret = AES_EncFinal(&AlgInfo, (unsigned char*)pOut+eelen, &DstLen);
	if( ret!=CTR_SUCCESS )	
	{
		//writelog(LOG_DEBUG, "AES_EncFinal() returns.");
		free (pOut);
		return -1;
	}

	eelen += DstLen;
	*dstLen = eelen;
	*dstString = pOut;

	return 0;
}

int AESDecode (BYTE *pUserKey, int keyLen, const char* srcString, int srcLen, char** dstString, int* dstLen)
{
	//FILE	*pIn, *pOut;
	char* pOut = 0;
	unsigned char UserKey[AES_USER_KEY_LEN]={0};
	unsigned char IV[AES_BLOCK_LEN]={0};
	//unsigned char SrcData[1024+32], DstData[1024+32];
	unsigned int  UKLen, IVLen;
	unsigned int SrcLen, DstLen;
	RET_VAL	ret;
	AES_ALG_INFO	AlgInfo;
	int ddlen = 0;

	SrcLen = srcLen;
	
	pOut = (char*)malloc(SrcLen+2);
	if (pOut == NULL) return -1;

	DstLen = SrcLen;

	UKLen = 16;
	IVLen = 16;
#ifdef AES_DEBUG
{
	int t = 0x10;
	int i;
	for (i=0; i<16; i++)
	{
		UserKey[i] = t+i;
	}
}
#else
	if (keyLen > 16) keyLen = 16;
	memcpy(UserKey, pUserKey, keyLen);
#endif

	AES_SetAlgInfo(AES_ModeType, AES_PadType, IV, &AlgInfo);

	//Decryption
	//if( ModeType==AI_ECB || ModeType==AI_CBC )
	ret = AES_DecKeySchedule(UserKey, UKLen, &AlgInfo);
	//else if( ModeType==AI_OFB || ModeType==AI_CFB )
	//	ret = AES_EncKeySchedule(UserKey, UKLen, &AlgInfo);

	if( ret!=CTR_SUCCESS )	
	{
		//writelog(LOG_DEBUG, "AES_DecKeySchedule() returns.");
		free (pOut);
		return -1;
	}

	ret = AES_DecInit(&AlgInfo);
	if( ret!=CTR_SUCCESS )	
	{
		//writelog(LOG_DEBUG, "AES_DecInit() returns.");
		free (pOut);
		return -1;
	}

	ret = AES_DecUpdate(&AlgInfo, (unsigned char*)srcString, SrcLen, (unsigned char*)pOut, &DstLen);
	if( ret!=CTR_SUCCESS )	
	{
		//writelog(LOG_DEBUG, "AES_DecUpdate() returns.");
		free (pOut);
		return -1;
	}
	ddlen = DstLen;

	ret = AES_DecFinal(&AlgInfo, (unsigned char*)pOut+ddlen, &DstLen);
	if( ret!=CTR_SUCCESS )	
	{
		//writelog(LOG_DEBUG, "AES_DecFinal() returns.");
		free (pOut);
		return -1;
	}
	ddlen += DstLen;
	*dstLen = ddlen;
	*dstString = pOut;
	return 0;

}

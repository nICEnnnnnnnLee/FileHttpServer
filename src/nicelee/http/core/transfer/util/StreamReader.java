package nicelee.http.core.transfer.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.PushbackInputStream;
import java.nio.ByteBuffer;

public class StreamReader {
	byte[] readBuffer = new byte[512];
	int maxSize = 2048;
	/**
	 *  从输入流中截取换行符\r\n, 读取前面的内容,并以String返回
	 *  maxSize后仍旧未读出换行符,抛出异常,
	 *  多余的内容在byteBuffer
	 * @param byteBuffer
	 * @return
	 * throws IOException
	 */
	public String readLineFromInputStream(ByteBuffer byteBuffer, BufferedInputStream in) throws IOException{
		int cnt = 0, rSize;
		//System.out.println("调用readLine方法");
		while(true) {
			//从byteBuffer中检查内容
			int position = byteBuffer.position();
			for(int i = 0 ; i< position; i++) {
				if( byteBuffer.get(i) == 13 && byteBuffer.get(i+1) == 10) {
					//构造String 
					String line = new String(byteBuffer.array(), 0, i);
					if(i + 2 <= position) {
						//将ByteBuffer多余的内容去掉
						byteBuffer.position(0);
						byteBuffer.put(byteBuffer.array(), i + 2, position - i - 1);
					}else {
						byteBuffer.clear();
					}
					//返回结果
					return line;
				}
			}
			if(byteBuffer.get(position) == 13) {
				int nextByte = in.read();
				if(nextByte == 10) {
					String line = new String(byteBuffer.array(), 0, position);
					byteBuffer.clear();
					return line;
				}else {
					byteBuffer.put((byte)nextByte);
				}
			}
			
			//从流中读取内容
			rSize = in.read(readBuffer);
			//System.out.println("读出字节数为: " +rSize);
			for(int i = 0 ; i< rSize - 1; i++) {
				//找到\r\n
				if(readBuffer[i] == 13 && readBuffer[i+1] == 10) {
					//构造String 
					byteBuffer.put(readBuffer, 0, i);
					String line = new String(byteBuffer.array(), 0, byteBuffer.position());
					
					//将从流中读取多余的内容移至ByteBuffer
					byteBuffer.clear();
					if( i+ 2 <= rSize - 1) {
						byteBuffer.put(readBuffer, i+ 2, rSize - 2 - i);
					}
					//返回结果
					return line;
				}
			}
			//未找到换行符
			//字符长度过大,返回null
			cnt += rSize;
			if(cnt > maxSize) {
				System.out.println("长度过大, 仍旧未找到回车换行符: " + maxSize);
				return null;
			}
			//将内容移至ByteBuffer
			byteBuffer.put(readBuffer, 0, rSize);
		}
	}
}

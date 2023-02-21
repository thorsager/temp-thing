/*
 * TutorialMicrophone.java
 * 
 * Copyright (c) 2003-2007 Avaya Inc. All rights reserved.
 * 
 * USE OR INSTALLATION OF THIS SAMPLE DEMONSTRATION SOFTWARE INDICATES THE END
 * USERS ACCEPTANCE OF THE GENERAL LICENSE TERMS AVAILABLE ON THE AVAYA WEBSITE
 * AT http://support.avaya.com/LicenseInfo/ (GENERAL LICENSE TERMS). DO NOT USE
 * THE SOFTWARE IF YOU DO NOT WISH TO BE BOUND BY THE GENERAL LICENSE TERMS. IN
 * ADDITION TO THE GENERAL LICENSE TERMS, THE FOLLOWING ADDITIONAL TERMS AND
 * RESTRICTIONS WILL TAKE PRECEDENCE AND APPLY TO THIS DEMONSTRATION SOFTWARE.
 * 
 * THIS DEMONSTRATION SOFTWARE IS PROVIDED FOR THE SOLE PURPOSE OF DEMONSTRATING
 * HOW TO USE THE SOFTWARE DEVELOPMENT KIT AND MAY NOT BE USED IN A LIVE OR
 * PRODUCTION ENVIRONMENT. THIS DEMONSTRATION SOFTWARE IS PROVIDED ON AN AS IS
 * BASIS, WITHOUT ANY WARRANTIES OR REPRESENTATIONS EXPRESS, IMPLIED, OR
 * STATUTORY, INCLUDING WITHOUT LIMITATION, WARRANTIES OF QUALITY, PERFORMANCE,
 * INFRINGEMENT, MERCHANTABILITY, OR FITNESS FOR A PARTICULAR PURPOSE.
 * 
 * EXCEPT FOR PERSONAL INJURY CLAIMS, WILLFUL MISCONDUCT AND END USERS VIOLATION
 * OF AVAYA OR ITS SUPPLIERS INTELLECTUAL PROPERTY RIGHTS, INCLUDING THROUGH A
 * BREACH OF THE SOFTWARE LICENSE, NEITHER AVAYA, ITS SUPPLIERS NOR END USER
 * SHALL BE LIABLE FOR (i) ANY INCIDENTAL, SPECIAL, STATUTORY, INDIRECT OR
 * CONSEQUENTIAL DAMAGES, OR FOR ANY LOSS OF PROFITS, REVENUE, OR DATA, TOLL
 * FRAUD, OR COST OF COVER AND (ii) DIRECT DAMAGES ARISING UNDER THIS AGREEMENT
 * IN EXCESS OF FIFTY DOLLARS (U.S. $50.00).
 * 
 * To the extent there is a conflict between the General License Terms, your
 * Customer Sales Agreement and the terms and restrictions set forth herein, the
 * terms and restrictions set forth herein shall prevail solely for this Utility
 * Demonstration Software.
 */
package sampleapps.clientmediastack;

import java.io.IOException;
import java.nio.ByteBuffer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

import com.avaya.api.media.audio.Audio;
import com.avaya.api.media.channels.AudioSource;
import com.avaya.mvcs.media.audio.ConversionTool;

/**
 * This is a Sample Tutorial Microphone Class. It just shows how to plug in a
 * microphone (MediaSource) to the Client Media Stack Audio RTP Stream. Do not 
 * use the code as is. You will need to tweak the microphone settings
 * including the AudioFormat for your system. 
 */
public class TutorialMicrophone implements AudioSource {
	
	// Constants for Audio Format
	private static final AudioFormat.Encoding ENCODING = 
											AudioFormat.Encoding.PCM_UNSIGNED;
	private static final int SAMPLE_SIZE_IN_BITS = 8;    // 8bits per PCM sample
	private static final float SAMPLE_RATE = 8000;        // 8Khz sampling rate
	private static final int CHANNELS = 1;               // monoaural system
	
	// sampleSizeInBytes * channels
	private static final int FRAME_SIZE = CHANNELS * SAMPLE_SIZE_IN_BITS / 8; 
	
	//sampleRate/frameSize;   frames per sec
	private static final float FRAME_RATE = SAMPLE_RATE;   
	
	// true for network byte order 
	private static final boolean BIG_ENDIAN = true;        
	
	private DataLine.Info targetInfo;
	private TargetDataLine targetDataLine;
	private AudioFormat audioFormat = null;
	private String codec = Audio.G711U;
	private int packetSize = 20; // in milliseconds
	
	public TutorialMicrophone(){
		
		audioFormat = new AudioFormat(ENCODING, SAMPLE_RATE,SAMPLE_SIZE_IN_BITS,
				CHANNELS,FRAME_SIZE,FRAME_RATE,BIG_ENDIAN);
		
		targetInfo = new DataLine.Info(TargetDataLine.class,audioFormat);
		
		if (!AudioSystem.isLineSupported(targetInfo)) {
			System.out.println("Error: Unsupported Line: " +targetInfo);
		}

	} 

	/**
	 * The Client Media Stack (i.e. The Audio RTP Transmitter) 
	 * uses the read method to get a ByteBuffer from the Microphone 
	 * (MediaSource)
	 */
	public int read(ByteBuffer dst) throws IOException {
        int bytesRead = 0;

        // Open the Microphone if not already open
		if(targetDataLine == null || !targetDataLine.isOpen()){
			try {
				targetDataLine = (TargetDataLine) 
											AudioSystem.getLine(targetInfo);
				targetDataLine.open(audioFormat);
				targetDataLine.start();			
			} catch (LineUnavailableException e) {
				System.out.println("WARNING: Unavailable Target Data Line");
//				e.printStackTrace();
				return bytesRead;
			} catch(Exception e){
                System.out.println("WARNING: Exception caught - "+e);
//				e.printStackTrace();
                return bytesRead;
			}
		}

		byte[] sounddata = dst.array();
		int offset = dst.position();
		int length = sounddata.length - offset;
		try{
			if(targetDataLine.available() >= length){
				
				// Read the Data from the Microphone
				bytesRead =  targetDataLine.read(sounddata, offset, length);
				
				// IMPORTANT:  The microphone reads data which is PCM_UNSIGNED
				// You will need to do the conversion to G.711U or G.711A or
				// G.729 or G.729A depending on which codec the Audio 
				// RTP Stream is using.
				// DO THE CONVERSION HERE
                if (getCodec() == Audio.G711U) {
//                    System.out.println("Converting from PCM to G.711U");
                    ConversionTool.pcm82ulaw(sounddata, offset, bytesRead, false);
                } else if (getCodec() == Audio.G711A) {
//                    System.out.println("Converting from PCM to G.711A");
                    ConversionTool.pcm82alaw(sounddata, offset, bytesRead, false);
                } else {
                    // other conversion method
                    System.out.println("WARNING: You need to provide your own conversion code for codec="+getCodec());
                }
                
                // move the position in the buffer to where we stopped writing
                dst.position(offset+bytesRead);
			}
		} catch(Exception e){
            System.out.println("WARNING: Exception caught - "+e);
//			e.printStackTrace();
		}
	
		return bytesRead;
	}

	/** 
	 * Closes the Microphone (and hence this MediaSource)
	 */
	public void close() throws IOException {
		if(targetDataLine != null)
			targetDataLine.close();
	}

	/** 
	 * Return the status of the Microphone
	 */
	public boolean isOpen() {
		return true;
	}

	/** 
	 * Sets the Codec type and packet Size of the Source. 
	 * Implement this only if needed. 
	 */
	public void setCodec(String codec, int packetSize) {
		if (!this.codec.equals(codec) || this.packetSize != packetSize) {
            System.out.println("Setting microphone codec to "+codec+", and packetSize to "+packetSize);
		    this.codec = codec;
		    this.packetSize = packetSize;
		}
	}

	/** 
	 * Gets the packet size of the Microphone
	 */
	public int getPacketSize() {
		return packetSize;
	}
	
	/**
	 * Gets the codec being used by the Microphone
	 */
	public String getCodec() {
		return codec;
	}
}

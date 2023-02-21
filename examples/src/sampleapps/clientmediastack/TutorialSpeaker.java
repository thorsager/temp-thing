/*
 * TutorialSpeaker.java
 * 
 * Copyright (c) 2002-2007 Avaya Inc. All rights reserved.
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
import javax.sound.sampled.SourceDataLine;

import com.avaya.api.media.audio.Audio;
import com.avaya.api.media.channels.AudioSink;
import com.avaya.mvcs.media.audio.Codec;
import com.avaya.mvcs.media.audio.ConversionTool;

/**
 * This is a Sample Tutorial Speaker Class. It just shows how to plug in a
 * speaker (MediaSink) to the Client Media Stack Audio RTP Stream. Do not 
 * use the code as is. You will need to tweak the speaker settings
 * including the AudioFormat for your system. 
 */
public class TutorialSpeaker implements AudioSink {

	// Constants for Audio Format
	private static final int SAMPLE_SIZE_IN_BITS = 8;    // 8bits per PCM sample
	private static final float SAMPLE_RATE = 8000;        // 8Khz sampling rate
	private static final int CHANNELS = 1;               // monoaural system
	
	// sampleSizeInBytes * channels
	private static final int FRAME_SIZE = CHANNELS * SAMPLE_SIZE_IN_BITS / 8; 
	
	//sampleRate/frameSize;   frames per sec
	private static final float FRAME_RATE = SAMPLE_RATE;   
	
	// true for network byte order 
	private static final boolean BIG_ENDIAN = true;        
	
	private SourceDataLine sourcedataline;
	
	private AudioFormat audioFormat = null;
	
	private DataLine.Info targetInfo = null;

	private String codec;

	private int packetSize;
	
	public TutorialSpeaker(){

		audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_UNSIGNED,
							SAMPLE_RATE,SAMPLE_SIZE_IN_BITS,
							CHANNELS,FRAME_SIZE,
							FRAME_RATE,BIG_ENDIAN);

	    targetInfo = new DataLine.Info(SourceDataLine.class,audioFormat);
		
		if (!AudioSystem.isLineSupported(targetInfo)) {
			System.out.println("WARNING: Unsupported Source Data Line Format");
		}
		
	}

	/**
	 * The Client Media Stack (i.e. The Audio RTP Receiver) 
	 * uses the write method to deliver a ByteBuffer to the Speaker (MediaSink)
	 */
	public int write(ByteBuffer src) throws IOException {
		
        int bytesWritten = 0;
        
		// Start the Speaker if not already open
		if((sourcedataline == null) || (!sourcedataline.isOpen())){
			try {
				sourcedataline = 
						(SourceDataLine)AudioSystem.getLine(targetInfo);
				sourcedataline.open(audioFormat);
				sourcedataline.start();
			} catch (LineUnavailableException e) {
                System.out.println("WARNING: Unavailable Target Data Line");
//                e.printStackTrace();
				return bytesWritten;
            } catch(Exception e){
                System.out.println("WARNING: Exception caught - "+e);
//                e.printStackTrace();
                return bytesWritten;
            }
		}
		
		try {
		    if(sourcedataline.available() >= src.remaining()) {			
                // IMPORTANT: The Speaker supports PCM_UNSIGNED.
                // You will need to do the conversion to G.711U or G.711A or
                // G.729 or G.729A depending on which codec the Audio
                // RTP Stream is using.
                // DO THE CONVERSION HERE
                Codec sourceCodec = Codec.getCodec(getCodec(), getPacketSize());
    
                byte[] sounddata = new byte[sourceCodec.getPacketSizeInBytes()];
    
                int size = (sounddata.length >= src.remaining()) ?
                            src.remaining() : sounddata.length;
                src.get(sounddata, 0, size);
    
                if (getCodec() == Audio.G711U) {
//                    System.out.println("Converting from G.711U to PCM");
                    ConversionTool.ulaw2pcm8(sounddata, 0, size, false);
                } else if (getCodec() == Audio.G711A) {
//                    System.out.println("Converting from G.711A to PCM");
                    ConversionTool.alaw2pcm8(sounddata, 0, size, false);
                } else {
                    // other conversion method
                    System.out.println("WARNING: You need to provide your own conversion code for codec="+getCodec());
                }
    
                // Writes the data to the Speaker
                bytesWritten = sourcedataline.write(sounddata,0,sounddata.length);
    		}
		} catch (Exception e) {
            System.out.println("WARNING: Exception caught - "+e);
//            e.printStackTrace();
		}
		
		return	bytesWritten;
	}

	/** 
	 * Closes the Speaker (and hence this MediaSink)
	 */
	public void close() throws IOException {
		if(sourcedataline != null)
			sourcedataline.close();		
	}

	/** 
	 * Return the status of the Speaker
	 */
	public boolean isOpen() {
			return true;
	}

	/** 
	 * Sets the Codec type and packet Size of the source. 
	 * This is the codec and packetSize of the incoming Audio RTP
	 * Stream.
	 */
	public void setCodec(String codec, int packetSize) {	
                System.out.println("Setting speaker codec to "+codec+", and packetSize to "+packetSize);
		this.codec = codec;
		this.packetSize = packetSize;
	}

	/** 
	 * Gets the codec of the Audio RTP Stream being used
	 * by the Speaker.
	 */
	public String getCodec() {
		return codec;
	}
	
	/**
	 * Gets the packetSize of the Audio RTP Stream being used by
	 * the Speaker.
	 */
	public int getPacketSize() {		
		return packetSize;
	}
}

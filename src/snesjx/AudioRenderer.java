/*
 * SnesJx - Portable Super Nintendo Entertainment System (TM) emulator.
 * 
 * (C) Copyright Nathan Cassano 2009
 *
 * Permission to use, copy, modify and distribute SnesJx in both binary and
 * source form, for non-commercial purposes, is hereby granted without fee,
 * providing that this license information and copyright notice appear with
 * all copies and any derived work.
 *
 * This software is provided 'as-is', without any express or implied
 * warranty. In no event shall the authors be held liable for any damages
 * arising from the use of this software.
 *
 * SnesJx is freeware for PERSONAL USE only. Commercial users should
 * seek permission of the copyright holders first. Commercial use includes
 * charging money for SnesJx or software derived from SnesJx.
 *
 * The copyright holders request that bug fixes and improvements to the code
 * should be forwarded to them so everyone can benefit from the modifications
 * in future versions.
 *
 * Super NES and Super Nintendo Entertainment System are trademarks of
 * Nintendo Co., Limited and its subsidiary companies.
 */

package snesjx;

import java.io.*;

class AudioRenderer
{
	private ShortArray soundbuffer;
	private int sample_count;
	private static final int RATE = 100;
	
	private Globals globals;
	private APU apu;
	
	AudioRenderer()
	{
		globals = Globals.globals;
		apu = globals.apu;

		sample_count = globals.sounddata.so.playback_rate / RATE;
		soundbuffer = new ShortArray(sample_count);
		
		globals.sounddata.so.err_rate = (int)(SnesSystem.SNES_SCANLINE_TIME * 0x10000 / (1.0 / (double) globals.sounddata.so.playback_rate));
	    
		globals.sounddata.SetEchoDelay(apu.DSP [APU.APU_EDL] & 0xf);
	    for (int i = 0; i < 8; i++)
	    {
	    	globals.sounddata.SetSoundFrequency(i, globals.sounddata.channels [i].hertz);
	    }
	}
	
	void RenderWave(String audio_id)
	{
		//int initPC = apu.PC - 1;
		
		String filename = audio_id + ".wav";
		
		File f = new File(filename);
		f.delete();
		
		try {
						
			RandomAccessFile ds = new RandomAccessFile(filename, "rw");
			
			// Add 40 bytes of space for wav header
			ds.write( new byte[40] );
			
			// Render until sample repeats
			//while ( IAPU.PC > initPC )
			for (int j = 0; j < 400; j++)
			{
				RenderSample();
				
				// Write output 
				for( int i = 0; i < soundbuffer.buffer.length; i++)
				{
					int s = soundbuffer.get16Bit(i) & 0xFFFF;
					ds.writeShort( ( ( s << 8 ) & 0xFF00) | s >>> 8 );
				}
			}

			// Add headers
			WriteWavHeader(ds);
			
			ds.close();
		   
		}
		catch (IOException ioe)
		{
			System.out.println( "IO error: " + ioe );
		}
		
		
		System.out.println( "Sound written" );
	}
	
	protected void WriteWavHeader(RandomAccessFile ds) throws IOException
	{
		byte channels = (byte) (globals.settings.Stereo ? 0x02 : 0x01);
		
		ds.seek(0);
		
		// String "RIFF"
		ds.write( new byte[] { 0x52, 0x49, 0x46, 0x46 } );

		// File size
		ds.writeInt( Integer.reverseBytes( (int) ds.length() - 8 ) );
		
		// String "WAVEfmt"
		ds.write( new byte[] { 0x57, 0x41, 0x56, 0x45, 0x66, 0x6d, 0x74, 0x20, 0x10, 0x0, 0x0, 0x0, 0x01, 0x0 } );
		
		// Channels
		ds.write( new byte[] { channels, 0x0 } );
		
		// Sample rate
		ds.writeInt( Integer.reverseBytes( globals.sounddata.so.playback_rate ) );

		// Bytes per second
		ds.writeInt( Integer.reverseBytes( globals.settings.SoundPlaybackRate * channels ) );

		// Bytes per sample
		ds.write( new byte[] { (byte) (channels * 2), 0x0} );
		
		// Bits per sample
		ds.write( new byte[] { (byte) (channels * 0x10), 0x0} );
		
		// String "data"
		ds.write( new byte[] { 0x64, 0x61, 0x74, 0x61 } );
		
		// Length of following data
		ds.writeInt( Integer.reverseBytes( (int) ds.length() - 40 ) );
	}
	
	void RenderSample()
	{		
		for (int j = 0; j < 2048000 / 32 / RATE; j++)
		{
			for (int i = 0; i < 32; i++)
			{
				apu.APU_EXECUTE1();
			}
			
			apu.TimerErrorCounter++;
			DoTimer();
		}
	    
	    globals.sounddata.MixSamples(soundbuffer, sample_count );
	
	}
	
	void DoTimer()
	{
		if (apu.TimerEnabled [2])
		{
			apu.Timer [2] += 4;

			if (apu.Timer[2] >= apu.TimerTarget[2])
			{
				apu.RAM.put8Bit(0xff, (apu.RAM.get8Bit(0xff) + 1) & 0xf);
				apu.Timer[2] -= apu.TimerTarget[2];
				apu.WaitCounter++;
				apu.APUExecuting = true;
			}
		}
		
		if (apu.TimerErrorCounter >= 8)	
		{
			apu.TimerErrorCounter = 0;
			if (apu.TimerEnabled [0])
			{
				apu.Timer[0]++;
				
				if (apu.Timer [0] >= apu.TimerTarget [0])
				{
					apu.RAM.put8Bit(0xfd, (apu.RAM.get8Bit(0xfd) + 1) & 0xf);
					apu.Timer[0] = 0;
					apu.WaitCounter++;
					apu.APUExecuting = true;
				}
			}

			apu.TimerErrorCounter = 0;
			
			if (apu.TimerEnabled [1])
			{
				apu.Timer[1]++;
				
				if (apu.Timer[1] >= apu.TimerTarget [1])
				{
					apu.RAM.put8Bit(0xfe, (apu.RAM.get8Bit(0xfe) + 1) & 0xf);
					apu.Timer[1] = 0;
					apu.WaitCounter++;
					apu.APUExecuting = true;
				}
			}
		}
	}
}

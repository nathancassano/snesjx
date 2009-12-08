/*
 * SnesJx - Portable Super Nintendo Entertainment System (TM) emulator.
 * 
 * (C) Copyright 2009 Nathan Cassano
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

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.swing.JFrame;

public class EngineRunnerJavaSE extends EngineRunner
{
	GLGameCanvas canvas = new GLGameCanvas();
	
	class GLGameCanvas extends GLCanvas implements GLEventListener, KeyListener
	{
		GLGameCanvas()
		{
			super(new GLCapabilities());
		}
		
		public void init(GLAutoDrawable drawable)
		{
			gamedisplay.init( new GLJogl( drawable.getContext() ) );
		}

		public void display(GLAutoDrawable drawable)
		{
			gamedisplay.display();
		}

		public void displayChanged(GLAutoDrawable drawable, boolean arg1, boolean arg2){}

		public void reshape(GLAutoDrawable drawable, int x, int y, int w, int h)
		{
			gamedisplay.reshape(x, y, drawable.getWidth(), drawable.getHeight());		
		}

		public void keyPressed(KeyEvent e) {}

		public void keyReleased(KeyEvent e) {}

		public void keyTyped(KeyEvent e) {}	
		
	}
	
	public static void main(String[] args)
	{
		// Run class
		EngineRunnerJavaSE engine = new EngineRunnerJavaSE();
		engine.Initialize( args );
		
		try
        {
	        Thread.sleep(1000);
        } catch (InterruptedException e)
        {
	        e.printStackTrace();
        }
		
		while (true)
		{
			engine.Execute();
		}
	}

	public EngineRunnerJavaSE()
	{
		super();
	}
	
	public String GetRomFilePath()
	{
		return "unittest/roms/SuperMarioWorld.smc";
	}

	public void InitDisplay()
	{
		canvas.addGLEventListener(canvas);
		canvas.addKeyListener(canvas);

		JFrame frame = new JFrame();
    	frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    	frame.getContentPane().add(canvas);
    	frame.setSize(512,512);
    	frame.setVisible(true);

	}
	
	public void EndScreenRefresh()
	{
		canvas.display();
		//Globals.globals.ppu.ResetBGPositionTracking();
	}

	public void StartScreenRefresh()
	{
		if( ++Globals.globals.ppu.FrameCount % Globals.globals.memory.ROMFramesPerSecond == 0 )
		{
			//globals.ppu.DisplayedRenderedFrameCount = globals.ppu.RenderedFramesCount;
			//globals.ppu.RenderedFramesCount = 0;
			Globals.globals.ppu.FrameCount = 0;
		}
	}
}

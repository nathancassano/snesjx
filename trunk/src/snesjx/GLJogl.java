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

import javax.media.opengl.GLContext;
import com.sun.opengl.impl.GLContextImpl;
import com.sun.opengl.impl.GLImpl;

final class GLJogl extends GLImpl implements GLGeneric
{

	public GLJogl(GLContext gl)
    {
	    super( (GLContextImpl) gl);
    }

	public void glOrthox(int left, int right, int bottom, int top, int near_val, int far_val)
    {
		super.glOrtho(left, right, bottom, top, near_val, far_val);
    }

	public void glClearDepthf(float depth)
    {
		super.glClearDepth(depth);
    }

	public void glTexCoordPointer(int size, int type, int stride, int ptr_buffer_offset)
    {
		super.glTexCoordPointer(size, type, stride, ptr_buffer_offset);
    }

	public void glVertexPointer(int size, int type, int stride, int ptr_buffer_offset)
    {
		super.glVertexPointer(size, type, stride, ptr_buffer_offset);
    }

	public void glClearColorx(int red, int green, int blue, int alpha)
    {
	    super.glClearColor((float)(red / 65536.0f), (float)(green / 65536.0f), (float)(blue / 65536.0f), (float)(alpha / 65536.0f) );
    }

	public void glTranslatex(int x, int y, int z)
	{
		super.glTranslatef(x, y, z);	
	}

}
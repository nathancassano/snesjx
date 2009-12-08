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

public interface GLGeneric
{
	public static final int GL_BYTE = 0x1400;
	public static final int GL_UNSIGNED_BYTE = 0x1401;
	public static final int GL_SHORT = 0x1402;
	public static final int GL_UNSIGNED_SHORT = 0x1403;
	public static final int GL_INT = 0x1404;
	public static final int GL_UNSIGNED_INT = 0x1405;
	public static final int GL_FLOAT = 0x1406;
	
	public static final int GL_FIXED = 0x140C;

	public static final int GL_TRIANGLES = 0x0004;
	
	public static final int GL_FLAT = 0x1D00;
	public static final int GL_SMOOTH = 0x1D01;
	
	public static final int GL_UNPACK_ALIGNMENT = 0x0CF5;
	
	public static final int GL_ARRAY_BUFFER = 0x8892;
	public static final int GL_ELEMENT_ARRAY_BUFFER = 0x8893;
	
	public static final int GL_DYNAMIC_DRAW = 0x88E8;
	public static final int GL_STATIC_DRAW = 0x88E4;
	
	public static final int GL_COLOR_BUFFER_BIT = 0x00004000;
	  
	public static final int GL_MATRIX_MODE = 0x0BA0;
	public static final int GL_MODELVIEW = 0x1700;
	public static final int GL_PROJECTION = 0x1701;
	public static final int GL_TEXTURE = 0x1702;
	
	public static final int GL_NEVER = 0x0200;
	public static final int GL_LESS = 0x0201;
	public static final int GL_EQUAL = 0x0202;
	public static final int GL_LEQUAL = 0x0203;
	public static final int GL_GREATER = 0x0204;
	public static final int GL_NOTEQUAL = 0x0205;
	public static final int GL_GEQUAL = 0x0206;
	public static final int GL_ALWAYS = 0x0207;
	public static final int GL_DEPTH_TEST = 0x0B71;
	public static final int GL_DEPTH_BITS = 0x0D56;
	public static final int GL_DEPTH_CLEAR_VALUE = 0x0B73;
	public static final int GL_DEPTH_FUNC = 0x0B74;
	public static final int GL_DEPTH_RANGE = 0x0B70;
	
	public static final int GL_ALPHA_TEST = 0x0BC0;
	public static final int GL_BLEND = 0x0BE2;
	public static final int GL_BLEND_SRC = 0x0BE1;
	public static final int GL_BLEND_DST = 0x0BE0;
	public static final int GL_ZERO = 0x0;
	public static final int GL_ONE = 0x1;
	public static final int GL_SRC_COLOR = 0x0300;
	public static final int GL_ONE_MINUS_SRC_COLOR = 0x0301;
	public static final int GL_SRC_ALPHA = 0x0302;
	
	public static final int GL_ALPHA = 0x1906;
	
	public static final int GL_COLOR = 0x1800;
	public static final int GL_DEPTH = 0x1801;
	
	public static final int GL_RGBA = 0x1908;
	
	public static final int GL_SCISSOR_TEST = 0x0C11;
	
	public static final int GL_PACK_ALIGNMENT = 0x0D05;
	
	public static final int GL_TEXTURE_ENV = 0x2300;

	public static final int GL_TEXTURE_2D = 0x0DE1;
	public static final int GL_TEXTURE_WRAP_S = 0x2802;
	public static final int GL_TEXTURE_WRAP_T = 0x2803;
	public static final int GL_TEXTURE_MAG_FILTER = 0x2800;
	public static final int GL_TEXTURE_MIN_FILTER = 0x2801;

	public static final int GL_NEAREST = 0x2600;
	public static final int GL_REPEAT = 0x2901;
	public static final int GL_CLAMP = 0x2900;
	
	public static final int GL_DEPTH_BUFFER_BIT = 0x00000100;
	
	public static final int GL_VERTEX_ARRAY = 0x8074;
	public static final int GL_INDEX_ARRAY = 0x8077;
	public static final int GL_TEXTURE_COORD_ARRAY = 0x8078;

	public static final int GL_UNSIGNED_SHORT_5_5_5_1 = 0x8034;
	
	public static final int GL_CLAMP_TO_EDGE = 0x812F;

	public void glAlphaFunc(int func, float ref);
	
	public void glBindBuffer(int target, int id);
	
	public void glBindTexture(int target, int texture);
	
	public void glBufferData(int target, int size, java.nio.Buffer data, int usage);
	
	public void glBufferSubData(int target, int offset, int size, java.nio.Buffer data);
	
	public void glClear(int mask);

	public void glClearColorx(int red, int green, int blue, int alpha);
	
	public void glClearDepthf(float depth);
	
	public void glColor4ub(byte red, byte green, byte blue, byte alpha );

	public void glColor4f(float f, float g, float h, float i) ;
	
	public void glDepthFunc(int func);
	
	public void glDisable(int cap);

	public void glDisableClientState(int cap);
	
	public void glDrawArrays(int mode, int first, int count);

	public void glEnable(int cap);
	
	public void glEnableClientState(int cap);
	
	public void glGenBuffers(int n, int[] ids, int ids_offset);
	
	public void glGenTextures(int n, int[] textures, int textures_offset);
	
	public void glLoadIdentity();
	
	public void glMatrixMode(int mode);
	
	public void glOrthox(int left, int right, int bottom, int top, int near_val, int far_val);
	
	public void glPixelStorei(int pname, int param);
	
	public void glPopMatrix();
	
	public void glPushMatrix();
	
	public void glRotatef(float angle, float x, float y, float z);
	
	public void glScalef(float x, float y, float z);
	
	public void glScissor(int x, int y, int width, int height);
	
	public void glShadeModel(int mode);
	
	public void glTexCoordPointer(int size, int type, int stride, java.nio.Buffer ptr);

	public void glTexCoordPointer(int size, int type, int stride, int ptr_buffer_offset);
	
	public void glTexImage2D(int target, int level, int internalFormat, int width, int height, int border, int format, int type, java.nio.Buffer pixels);
	
	public void glTexParameteri(int target, int pname, int param);
	
	public void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, java.nio.Buffer pixels);
	
	public void glTranslatex(int x, int y, int z);
	
	public void glVertexPointer(int size, int type, int stride, java.nio.Buffer ptr);
	
	public void glVertexPointer(int size, int type, int stride, int ptr_buffer_offset);
	
	public void glViewport(int x, int y, int width, int height);
}

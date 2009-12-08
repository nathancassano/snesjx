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

class DSP4
{
	boolean waiting4command;
	private boolean half_command;
	private int command;
	private int in_count;
	private int in_index;
	private int out_count;
	private int out_index;
	private ByteArray parameters = new ByteArray(512);
	private ByteArray output = new ByteArray(512);
	
	private int dsp4_byte;
	private int dsp4_address;
	
	private int DSP4_Logic;						// controls op flow

	// projection format
	private short lcv;									// loop-control variable
	private short distance;						 // z-position into virtual world
	private short raster;							 // current raster line
	private short segments;						 // number of raster lines drawn

	// 1.15.16 or 1.15.0 [sign, integer, fraction]
	private int world_x;							// line of x-projection in world
	private int world_y;							// line of y-projection in world
	private int world_dx;						 // projection line x-delta
	private int world_dy;						 // projection line y-delta
	private short world_ddx;						// x-delta increment
	private short world_ddy;						// y-delta increment
	private int world_xenv;					 // world x-shaping factor
	private short world_yofs;					 // world y-vertical scroll

	private short view_x1;							// current viewer-x
	private short view_y1;							// current viewer-y
	private short view_x2;							// future viewer-x
	private short view_y2;							// future viewer-y
	private short view_dx;							// view x-delta factor
	private short view_dy;							// view y-delta factor
	private short view_xofs1;					 // current viewer x-vertical scroll
	private short view_yofs1;					 // current viewer y-vertical scroll
	private short view_xofs2;					 // future viewer x-vertical scroll
	private short view_yofs2;					 // future viewer y-vertical scroll
	private short view_yofsenv;				 // y-scroll shaping factor
	private short view_turnoff_x;			 // road turnoff data
	private short view_turnoff_dx;			// road turnoff delta factor

	// drawing area
	private short viewport_cx;					// x-center of viewport window
	private short viewport_cy;					// y-center of render window
	private short viewport_left;				// x-left of viewport
	private short viewport_right;			 // x-right of viewport
	private short viewport_top;				 // y-top of viewport
	private short viewport_bottom;			// y-bottom of viewport

	// sprite structure
	private short sprite_x;						 // projected x-pos of sprite
	private short sprite_y;						 // projected y-pos of sprite
	private short sprite_attr;					// obj attributes
	private boolean sprite_size;					// sprite size: 8x8 or 16x16
	private short sprite_clipy;				 // visible line to clip pixels off
	private short sprite_count;


	// generic projection variables designed for
	// two solid polygons + two polygon sides
	private short poly_clipLf[][]	= new short[2][2];		// left clip boundary
	private short poly_clipRt[][]	= new short[2][2];		// right clip boundary
	private short poly_ptr[][]		= new short[2][2];			 // HDMA structure pointers
	private short poly_raster[][]	= new short[2][2];		// current raster line below horizon
	private short poly_top[][]		= new short[2][2];			 // top clip boundary
	private short poly_bottom[][]	= new short[2][2];		// bottom clip boundary
	private short poly_cx[][]		= new short[2][2];				// center for left/right points
	private short poly_start[]		= new short[2];				// current projection points
	private short poly_plane[]		= new short[2];				// previous z-plane distance

	// OAM
	private short OAM_attr[] = new short[16];
	private short OAM_index;
	private short OAM_bits;

	private short OAM_RowMax;
	private short OAM_Row[] = new short[32];
	
	private static final int div_lut[] =
	{
		0x0000, 0x8000, 0x4000, 0x2aaa, 0x2000, 0x1999, 0x1555, 0x1249, 0x1000, 0x0e38,
		0x0ccc, 0x0ba2, 0x0aaa, 0x09d8, 0x0924, 0x0888, 0x0800, 0x0787, 0x071c, 0x06bc,
		0x0666, 0x0618, 0x05d1, 0x0590, 0x0555, 0x051e, 0x04ec, 0x04bd, 0x0492, 0x0469,
		0x0444, 0x0421, 0x0400, 0x03e0, 0x03c3, 0x03a8, 0x038e, 0x0375, 0x035e, 0x0348,
		0x0333, 0x031f, 0x030c, 0x02fa, 0x02e8, 0x02d8, 0x02c8, 0x02b9, 0x02aa, 0x029c,
		0x028f, 0x0282, 0x0276, 0x026a, 0x025e, 0x0253, 0x0249, 0x023e, 0x0234, 0x022b,
		0x0222, 0x0219, 0x0210, 0x0208
	};
	
	private static final int OP0A_Values[] =
	{
		0x0000, 0x0030, 0x0060, 0x0090, 0x00c0, 0x00f0, 0x0120, 0x0150, 0xfe80,
		0xfeb0, 0xfee0, 0xff10, 0xff40, 0xff70, 0xffa0, 0xffd0
	};

	void InitDSP4()
	{
		//TODO: Zero out all members 
		//memset(&DSP4, 0, sizeof(DSP4));
		waiting4command = true;
	}
	
	private void DSP4_SetByte()
	{
		// clear pending read
		if (out_index < out_count)
		{
			out_index++;
			return;
		}

		if (waiting4command)
		{
			if (half_command)
			{
				command |= (dsp4_byte << 8);
				in_index = 0;
				waiting4command = false;
				half_command = false;
				out_count = 0;
				out_index = 0;

				DSP4_Logic = 0;


				switch (command)
				{
					case 0x0000:
						in_count = 4; break;
					case 0x0001:
						in_count = 44; break;
					case 0x0003:
						in_count = 0; break;
					case 0x0005:
						in_count = 0; break;
					case 0x0006:
						in_count = 0; break;
					case 0x0007:
						in_count = 34; break;
					case 0x0008:
						in_count = 90; break;
					case 0x0009:
						in_count = 14; break;
					case 0x000a:
						in_count = 6; break;
					case 0x000b:
						in_count = 6; break;
					case 0x000d:
						in_count = 42; break;
					case 0x000e:
						in_count = 0; break;
					case 0x000f:
						in_count = 46; break;
					case 0x0010:
						in_count = 36; break;
					case 0x0011:
						in_count = 8; break;
					default:
						waiting4command = true;
						break;
				}
			}
			else
			{
				command = dsp4_byte;
				half_command = true;
			}
		}
		else
		{
			parameters.put8Bit(in_index, dsp4_byte);
			in_index++;
		}

		if (!waiting4command && in_count == in_index)
		{
			// Actually execute the command
			waiting4command = true;
			out_index = 0;
			in_index = 0;

			switch (command)
			{
					// 16-bit multiplication
				case 0x0000:
				{
					int multiplier, multiplicand;
					int product;

					multiplier = DSP4_READ_WORD();
					multiplicand = DSP4_READ_WORD();

					product = DSP4_Multiply(multiplicand, multiplier);

					DSP4_CLEAR_OUT();
					DSP4_WRITE_WORD(product);
					DSP4_WRITE_WORD(product >> 16);
				}
				break;
				
				//TODO: Finish DSP4_SetByte routine

				// single-player track projection
				case 0x0001:
					DSP4_OP01(); break;

				// single-player selection
				case 0x0003:
					DSP4_OP03(); break;

				// clear OAM
				case 0x0005:
					DSP4_OP05(); break;

				// transfer OAM
				case 0x0006:
					DSP4_OP06(); break;

				// single-player track turnoff projection
				case 0x0007:
					DSP4_OP07(); break;

				// solid polygon projection
				case 0x0008:
					DSP4_OP08(); break;

				// sprite projection
				case 0x0009:
					DSP4_OP09(); break;

				// unknown
				case 0x000A:
				{
					int in2a = DSP4_READ_WORD();
					int out1a, out2a, out3a, out4a;

					//NAC: Inlined DSP4_OP0A(in2a, &out2a, &out1a, &out4a, &out3a);
					out2a = OP0A_Values[(in2a & 0xf000) >> 12];
					out1a = OP0A_Values[(in2a & 0x0f00) >> 8];
					out4a = OP0A_Values[(in2a & 0x00f0) >> 4];
					out3a = OP0A_Values[(in2a & 0x000f)];

					DSP4_CLEAR_OUT();
					DSP4_WRITE_WORD(out1a);
					DSP4_WRITE_WORD(out2a);
					DSP4_WRITE_WORD(out3a);
					DSP4_WRITE_WORD(out4a);
				}
				break;

				// set OAM
				case 0x000B:
				{
					short sp_x = DSP4_READ_WORD();
					short sp_y = DSP4_READ_WORD();
					short sp_attr = DSP4_READ_WORD();
					boolean draw = true;

					DSP4_CLEAR_OUT();

					draw = DSP4_OP0B(draw, sp_x, sp_y, sp_attr, false, true);
				}
				break;

				// multi-player track projection
				case 0x000D:
					DSP4_OP0D(); break;

				// multi-player selection
				case 0x000E:
					DSP4_OP0E(); break;

				// single-player track projection with lighting
				case 0x000F:
					DSP4_OP0F(); break;

				// single-player track turnoff projection with lighting
				case 0x0010:
					DSP4_OP10(); break;

				// unknown: horizontal mapping command
				case 0x0011:
				{
					int a, b, c, d, m;

					d = DSP4_READ_WORD();
					c = DSP4_READ_WORD();
					b = DSP4_READ_WORD();
					a = DSP4_READ_WORD();

					m = DSP4_OP11(a, b, c, d);

					DSP4_CLEAR_OUT();
					DSP4_WRITE_WORD(m);

					break;
				}

				default:
					break;
			}
			
		}
	}

	private int DSP4_Inverse(int value)
	{
		// saturate bounds
		if (value < 0)
		{
			value = 0;
		}
		
		if (value > 63)
		{
			value = 63;
		}
		
		return div_lut[value];
	}

	void DSP4_OP01()
	{
		waiting4command = false;

		// op flow control
		switch (DSP4_Logic)
		{
			// TODO: Figure out how to do goto's
			case 1:
				//goto resume1;
				break;
			case 2:
				//goto resume2;
				break;
			case 3:
				//goto resume3;
				break;
		}

		////////////////////////////////////////////////////
		// process initial inputs

		// sort inputs
		world_y = DSP4_READ_DWORD();
		poly_bottom[0][0] = DSP4_READ_WORD();
		poly_top[0][0] = DSP4_READ_WORD();
		poly_cx[1][0] = DSP4_READ_WORD();
		viewport_bottom = DSP4_READ_WORD();
		world_x = DSP4_READ_DWORD();
		poly_cx[0][0] = DSP4_READ_WORD();
		poly_ptr[0][0] = DSP4_READ_WORD();
		world_yofs = DSP4_READ_WORD();
		world_dy = DSP4_READ_DWORD();
		world_dx = DSP4_READ_DWORD();
		distance = DSP4_READ_WORD();
		DSP4_READ_WORD(); // 0x0000
		world_xenv = DSP4_READ_DWORD();
		world_ddy = DSP4_READ_WORD();
		world_ddx = DSP4_READ_WORD();
		view_yofsenv = DSP4_READ_WORD();

		// initial (x,y,offset) at starting raster line
		view_x1 = (short) ((world_x + world_xenv) >> 16);
		view_y1 = (short) (world_y >> 16);
		view_xofs1 = (short) (world_x >> 16);
		view_yofs1 = world_yofs;
		view_turnoff_x = 0;
		view_turnoff_dx = 0;

		// first raster line
		poly_raster[0][0] = poly_bottom[0][0];

		do
		{
			////////////////////////////////////////////////////
			// process one iteration of projection

			// perspective projection of world (x,y,scroll) points
			// based on the current projection lines
			view_x2 = (short) (( ( ( world_x + world_xenv ) >> 16 ) * distance >> 15 ) + ( view_turnoff_x * distance >> 15 ));
			view_y2 = (short) ((world_y >> 16) * distance >> 15);
			view_xofs2 = view_x2;
			view_yofs2 = (short) ((world_yofs * distance >> 15) + poly_bottom[0][0] - view_y2);


			// 1. World x-location before transformation
			// 2. Viewer x-position at the next
			// 3. World y-location before perspective projection
			// 4. Viewer y-position below the horizon
			// 5. Number of raster lines drawn in this iteration

			DSP4_CLEAR_OUT();
			DSP4_WRITE_WORD((world_x + world_xenv) >> 16);
			DSP4_WRITE_WORD(view_x2);
			DSP4_WRITE_WORD(world_y >> 16);
			DSP4_WRITE_WORD(view_y2);

			//////////////////////////////////////////////////////

			// SR = 0x00

			// determine # of raster lines used
			segments = (short) (poly_raster[0][0] - view_y2);

			// prevent overdraw
			if (view_y2 >= poly_raster[0][0])
				segments = 0;
			else
				poly_raster[0][0] = view_y2;

			// don't draw outside the window
			if (view_y2 < poly_top[0][0])
			{
				segments = 0;

				// flush remaining raster lines
				if (view_y1 >= poly_top[0][0])
					segments = (short) (view_y1 - poly_top[0][0]);
			}

			// SR = 0x80

			DSP4_WRITE_WORD(segments);

			//////////////////////////////////////////////////////

			// scan next command if no SR check needed
			if (segments > 0)
			{
				int px_dx, py_dy;
				int x_scroll, y_scroll;

				// SR = 0x00

				// linear interpolation (lerp) between projected points
				px_dx = (view_xofs2 - view_xofs1) * DSP4_Inverse(segments) << 1;
				py_dy = (view_yofs2 - view_yofs1) * DSP4_Inverse(segments) << 1;

				// starting step values
				x_scroll = SEX16(poly_cx[0][0] + view_xofs1);
				y_scroll = SEX16(-viewport_bottom + view_yofs1 + view_yofsenv + poly_cx[1][0] - world_yofs);

				// SR = 0x80

				// rasterize line
				for (lcv = 0; lcv < segments; lcv++)
				{
					// 1. HDMA memory pointer (bg1)
					// 2. vertical scroll offset ($210E)
					// 3. horizontal scroll offset ($210D)

					DSP4_WRITE_WORD(poly_ptr[0][0]);
					DSP4_WRITE_WORD((y_scroll + 0x8000) >> 16);
					DSP4_WRITE_WORD((x_scroll + 0x8000) >> 16);


					// update memory address
					poly_ptr[0][0] -= 4;

					// update screen values
					x_scroll += px_dx;
					y_scroll += py_dy;
				}
			}

			////////////////////////////////////////////////////
			// Post-update

			// update new viewer (x,y,scroll) to last raster line drawn
			view_x1 = view_x2;
			view_y1 = view_y2;
			view_xofs1 = view_xofs2;
			view_yofs1 = view_yofs2;

			// add deltas for projection lines
			world_dx += SEX78(world_ddx);
			world_dy += SEX78(world_ddy);

			// update projection lines
			world_x += (world_dx + world_xenv);
			world_y += world_dy;

			// update road turnoff position
			view_turnoff_x += view_turnoff_dx;

			////////////////////////////////////////////////////
			// command check

			// scan next command
			in_count = 2;
			DSP4_WAIT(1);
			resume1 :

			// check for termination
			distance = DSP4_READ_WORD();
			if (distance == -0x8000)
				break;

			// road turnoff
			if( distance == 0x8001 )
			{
				in_count = 6;
				DSP4_WAIT(2);
				resume2:

				distance = DSP4_READ_WORD();
				view_turnoff_x = DSP4_READ_WORD();
				view_turnoff_dx = DSP4_READ_WORD();

				// factor in new changes
				view_x1 += ( view_turnoff_x * distance >> 15 );
				view_xofs1 += ( view_turnoff_x * distance >> 15 );

				// update stepping values
				view_turnoff_x += view_turnoff_dx;

				in_count = 2;
				DSP4_WAIT(1);
			}

			// already have 2 bytes read
			in_count = 6;
			DSP4_WAIT(3);
			resume3 :

			// inspect inputs
			world_ddy = DSP4_READ_WORD();
			world_ddx = DSP4_READ_WORD();
			view_yofsenv = DSP4_READ_WORD();

			// no envelope here
			world_xenv = 0;
		}
		while ( true );

		// terminate op
		waiting4command = true;
	}

	void DSP4_OP0E()
	{
		OAM_RowMax = 16;
		for(int i = 0; i < OAM_Row.length; i++)
			OAM_Row[i] = 0;
	}
	
	private void DSP4_GetByte()
	{
		if (out_count > 0)
		{
			dsp4_byte = output.get8Bit(out_index&0x1FF);
			out_index++;
			if (out_count == out_index)
				out_count = 0;
		}
		else
		{
			dsp4_byte = 0xff;
		}
	}

	private void DSP4_OP03()
	{
	  OAM_RowMax = 33;
	  for(int i = 0; i < OAM_Row.length; i++)
		  OAM_Row[i] = 0;
	}

	private void DSP4_OP05()
	{
		OAM_index = 0;
		OAM_bits = 0;
		for(int i = 0; i < OAM_Row.length; i++)
			OAM_Row[i] = 0;
		sprite_count = 0;
	}

	private void DSP4_OP06()
	{
		DSP4_CLEAR_OUT();
		DSP4_WRITE_16_WORD(OAM_attr);
	}
	
	void DSP4_OP07()
	{
		waiting4command = false;

		// op flow control
		switch (DSP4_Logic)
		{
			// TODO: Figure out how to do goto's
			case 1:
				//goto resume1;
				break;
			case 2:
				//goto resume2;
				break;
		}

		////////////////////////////////////////////////////
		// sort inputs

		world_y = DSP4_READ_DWORD();
		poly_bottom[0][0] = DSP4_READ_WORD();
		poly_top[0][0] = DSP4_READ_WORD();
		poly_cx[1][0] = DSP4_READ_WORD();
		viewport_bottom = DSP4_READ_WORD();
		world_x = DSP4_READ_DWORD();
		poly_cx[0][0] = DSP4_READ_WORD();
		poly_ptr[0][0] = DSP4_READ_WORD();
		world_yofs = DSP4_READ_WORD();
		distance = DSP4_READ_WORD();
		view_y2 = DSP4_READ_WORD();
		view_dy = (short) (DSP4_READ_WORD() * distance >> 15);
		view_x2 = DSP4_READ_WORD();
		view_dx = (short) (DSP4_READ_WORD() * distance >> 15);
		view_yofsenv = DSP4_READ_WORD();

		// initial (x,y,offset) at starting raster line
		view_x1 = (short) (world_x >> 16);
		view_y1 = (short) (world_y >> 16);
		view_xofs1 = view_x1;
		view_yofs1 = world_yofs;

		// first raster line
		poly_raster[0][0] = poly_bottom[0][0];


		do
		{
			////////////////////////////////////////////////////
			// process one iteration of projection

			// add shaping
			view_x2 += view_dx;
			view_y2 += view_dy;

			// vertical scroll calculation
			view_xofs2 = view_x2;
			view_yofs2 = (short) ((world_yofs * distance >> 15) + poly_bottom[0][0] - view_y2);

			// 1. Viewer x-position at the next
			// 2. Viewer y-position below the horizon
			// 3. Number of raster lines drawn in this iteration

			DSP4_CLEAR_OUT();
			DSP4_WRITE_WORD(view_x2);
			DSP4_WRITE_WORD(view_y2);

			//////////////////////////////////////////////////////

			// SR = 0x00

			// determine # of raster lines used
			segments = (short) (view_y1 - view_y2);

			// prevent overdraw
			if (view_y2 >= poly_raster[0][0])
				segments = 0;
			else
				poly_raster[0][0] = view_y2;

			// don't draw outside the window
			if (view_y2 < poly_top[0][0])
			{
				segments = 0;

				// flush remaining raster lines
				if (view_y1 >= poly_top[0][0])
					segments = (short) (view_y1 - poly_top[0][0]);
			}

			// SR = 0x80

			DSP4_WRITE_WORD(segments);

			//////////////////////////////////////////////////////

			// scan next command if no SR check needed
			if (segments > 0)
			{
				int px_dx, py_dy;
				int x_scroll, y_scroll;

				// SR = 0x00

				// linear interpolation (lerp) between projected points
				px_dx = (view_xofs2 - view_xofs1) * DSP4_Inverse(segments) << 1;
				py_dy = (view_yofs2 - view_yofs1) * DSP4_Inverse(segments) << 1;

				// starting step values
				x_scroll = SEX16(poly_cx[0][0] + view_xofs1);
				y_scroll = SEX16(-viewport_bottom + view_yofs1 + view_yofsenv + poly_cx[1][0] - world_yofs);

				// SR = 0x80

				// rasterize line
				for (lcv = 0; lcv < segments; lcv++)
				{
					// 1. HDMA memory pointer (bg2)
					// 2. vertical scroll offset ($2110)
					// 3. horizontal scroll offset ($210F)

					DSP4_WRITE_WORD(poly_ptr[0][0]);
					DSP4_WRITE_WORD((y_scroll + 0x8000) >> 16);
					DSP4_WRITE_WORD((x_scroll + 0x8000) >> 16);

					// update memory address
					poly_ptr[0][0] -= 4;

					// update screen values
					x_scroll += px_dx;
					y_scroll += py_dy;
				}
			}

			/////////////////////////////////////////////////////
			// Post-update

			// update new viewer (x,y,scroll) to last raster line drawn
			view_x1 = view_x2;
			view_y1 = view_y2;
			view_xofs1 = view_xofs2;
			view_yofs1 = view_yofs2;

			////////////////////////////////////////////////////
			// command check

			// scan next command
			in_count = 2;
			DSP4_WAIT(1);
			
			resume1 :

			// check for opcode termination
			distance = DSP4_READ_WORD();
			if (distance == -0x8000)
				break;

			// already have 2 bytes in queue
			in_count = 10;
			DSP4_WAIT(2);
			
			resume2 :

			// inspect inputs
			view_y2 = DSP4_READ_WORD();
			view_dy = (short) (DSP4_READ_WORD() * distance >> 15);
			view_x2 = DSP4_READ_WORD();
			view_dx = (short) (DSP4_READ_WORD() * distance >> 15);
			view_yofsenv = DSP4_READ_WORD();
		}
		while (true);

		waiting4command = true;
	}
	
	void DSP4_OP0F()
	{
		waiting4command = false;

		// op flow control
		switch (DSP4_Logic)
		{
			// TODO: Figure out how to do goto's
			case 1:
				//goto resume1;
				break;
			case 2:
				//goto resume2;
				break;
			case 3:
				//goto resume3;
				break;
			case 4:
				//goto resume4;
				break;
		}

		////////////////////////////////////////////////////
		// process initial inputs

		// sort inputs
		DSP4_READ_WORD(); // 0x0000
		world_y = DSP4_READ_DWORD();
		poly_bottom[0][0] = DSP4_READ_WORD();
		poly_top[0][0] = DSP4_READ_WORD();
		poly_cx[1][0] = DSP4_READ_WORD();
		viewport_bottom = DSP4_READ_WORD();
		world_x = DSP4_READ_DWORD();
		poly_cx[0][0] = DSP4_READ_WORD();
		poly_ptr[0][0] = DSP4_READ_WORD();
		world_yofs = DSP4_READ_WORD();
		world_dy = DSP4_READ_DWORD();
		world_dx = DSP4_READ_DWORD();
		distance = DSP4_READ_WORD();
		DSP4_READ_WORD(); // 0x0000
		world_xenv = DSP4_READ_DWORD();
		world_ddy = DSP4_READ_WORD();
		world_ddx = DSP4_READ_WORD();
		view_yofsenv = DSP4_READ_WORD();

		// initial (x,y,offset) at starting raster line
		view_x1 = (short) ((world_x + world_xenv) >> 16);
		view_y1 = (short) (world_y >> 16);
		view_xofs1 = (short) (world_x >> 16);
		view_yofs1 = world_yofs;
		view_turnoff_x = 0;
		view_turnoff_dx = 0;

		// first raster line
		poly_raster[0][0] = poly_bottom[0][0];


		do
		{
			////////////////////////////////////////////////////
			// process one iteration of projection

			// perspective projection of world (x,y,scroll) points
			// based on the current projection lines
			view_x2 = (short) (((world_x + world_xenv) >> 16) * distance >> 15);
			view_y2 = (short) ((world_y >> 16) * distance >> 15);
			view_xofs2 = view_x2;
			view_yofs2 = (short) ((world_yofs * distance >> 15) + poly_bottom[0][0] - view_y2);

			// 1. World x-location before transformation
			// 2. Viewer x-position at the next
			// 3. World y-location before perspective projection
			// 4. Viewer y-position below the horizon
			// 5. Number of raster lines drawn in this iteration

			DSP4_CLEAR_OUT();
			DSP4_WRITE_WORD((world_x + world_xenv) >> 16);
			DSP4_WRITE_WORD(view_x2);
			DSP4_WRITE_WORD(world_y >> 16);
			DSP4_WRITE_WORD(view_y2);

			//////////////////////////////////////////////////////

			// SR = 0x00

			// determine # of raster lines used
			segments = (short) (poly_raster[0][0] - view_y2);

			// prevent overdraw
			if (view_y2 >= poly_raster[0][0])
				segments = 0;
			else
				poly_raster[0][0] = view_y2;

			// don't draw outside the window
			if (view_y2 < poly_top[0][0])
			{
				segments = 0;

				// flush remaining raster lines
				if (view_y1 >= poly_top[0][0])
					segments = (short) (view_y1 - poly_top[0][0]);
			}

			// SR = 0x80

			DSP4_WRITE_WORD(segments);

			//////////////////////////////////////////////////////

			// scan next command if no SR check needed
			if (segments > 0)
			{
				int px_dx, py_dy;
				int x_scroll, y_scroll;

				for (lcv = 0; lcv < 4; lcv++)
				{
					// grab inputs
					in_count = 4;
					DSP4_WAIT(1);
					resume1 :
					for (;;)
					{
						short distance;
						short color, red, green, blue;

						distance = DSP4_READ_WORD();
						color = DSP4_READ_WORD();

						// U1+B5+G5+R5
						red = (short) (color & 0x1f);
						green = (short) ((color >> 5) & 0x1f);
						blue = (short) ((color >> 10) & 0x1f);

						// dynamic lighting
						red = (short) ((red * distance >> 15) & 0x1f);
						green = (short) ((green * distance >> 15) & 0x1f);
						blue = (short) ((blue * distance >> 15) & 0x1f);
						color = (short) (red | (green << 5) | (blue << 10));

						DSP4_CLEAR_OUT();
						DSP4_WRITE_WORD(color);
						break;
					}
				}

				//////////////////////////////////////////////////////

				// SR = 0x00

				// linear interpolation (lerp) between projected points
				px_dx = (view_xofs2 - view_xofs1) * DSP4_Inverse(segments) << 1;
				py_dy = (view_yofs2 - view_yofs1) * DSP4_Inverse(segments) << 1;


				// starting step values
				x_scroll = SEX16(poly_cx[0][0] + view_xofs1);
				y_scroll = SEX16(-viewport_bottom + view_yofs1 + view_yofsenv + poly_cx[1][0] - world_yofs);

				// SR = 0x80

				// rasterize line
				for (lcv = 0; lcv < segments; lcv++)
				{
					// 1. HDMA memory pointer
					// 2. vertical scroll offset ($210E)
					// 3. horizontal scroll offset ($210D)

					DSP4_WRITE_WORD(poly_ptr[0][0]);
					DSP4_WRITE_WORD((y_scroll + 0x8000) >> 16);
					DSP4_WRITE_WORD((x_scroll + 0x8000) >> 16);

					// update memory address
					poly_ptr[0][0] -= 4;

					// update screen values
					x_scroll += px_dx;
					y_scroll += py_dy;
				}
			}

			////////////////////////////////////////////////////
			// Post-update

			// update new viewer (x,y,scroll) to last raster line drawn
			view_x1 = view_x2;
			view_y1 = view_y2;
			view_xofs1 = view_xofs2;
			view_yofs1 = view_yofs2;

			// add deltas for projection lines
			world_dx += SEX78(world_ddx);
			world_dy += SEX78(world_ddy);

			// update projection lines
			world_x += (world_dx + world_xenv);
			world_y += world_dy;

			// update road turnoff position
			view_turnoff_x += view_turnoff_dx;

			////////////////////////////////////////////////////
			// command check

			// scan next command
			in_count = 2;
			DSP4_WAIT(2);
			resume2:

			// check for termination
			distance = DSP4_READ_WORD();
			if (distance == -0x8000)
				break;

			// road splice
			if( distance == 0x8001 )
			{
				in_count = 6;
				DSP4_WAIT(3);
				resume3:

				distance = DSP4_READ_WORD();
				view_turnoff_x = DSP4_READ_WORD();
				view_turnoff_dx = DSP4_READ_WORD();

				// factor in new changes
				view_x1 += ( view_turnoff_x * distance >> 15 );
				view_xofs1 += ( view_turnoff_x * distance >> 15 );

				// update stepping values
				view_turnoff_x += view_turnoff_dx;

				in_count = 2;
				DSP4_WAIT(2);
			}

			// already have 2 bytes in queue
			in_count = 6;
			DSP4_WAIT(4);
			resume4 :

			// inspect inputs
			world_ddy = DSP4_READ_WORD();
			world_ddx = DSP4_READ_WORD();
			view_yofsenv = DSP4_READ_WORD();

			// no envelope here
			world_xenv = 0;
		}
		while ( true );

		// terminate op
		waiting4command = true;
	}
	
	void DSP4_OP10()
	{
		waiting4command = false;

		// op flow control
		switch (DSP4_Logic)
		{
			// TODO: Figure out how to do goto's
			case 1:
				//goto resume1;
				break;
			case 2:
				//goto resume2;
				break;
			case 3:
				//goto resume3;
				break;
		}

		////////////////////////////////////////////////////
		// sort inputs

		DSP4_READ_WORD(); // 0x0000
		world_y = DSP4_READ_DWORD();
		poly_bottom[0][0] = DSP4_READ_WORD();
		poly_top[0][0] = DSP4_READ_WORD();
		poly_cx[1][0] = DSP4_READ_WORD();
		viewport_bottom = DSP4_READ_WORD();
		world_x = DSP4_READ_DWORD();
		poly_cx[0][0] = DSP4_READ_WORD();
		poly_ptr[0][0] = DSP4_READ_WORD();
		world_yofs = DSP4_READ_WORD();
		distance = DSP4_READ_WORD();
		view_y2 = DSP4_READ_WORD();
		view_dy = (short) (DSP4_READ_WORD() * distance >> 15);
		view_x2 = DSP4_READ_WORD();
		view_dx = (short) (DSP4_READ_WORD() * distance >> 15);
		view_yofsenv = DSP4_READ_WORD();

		// initial (x,y,offset) at starting raster line
		view_x1 = (short) (world_x >> 16);
		view_y1 = (short) (world_y >> 16);
		view_xofs1 = view_x1;
		view_yofs1 = world_yofs;

		// first raster line
		poly_raster[0][0] = poly_bottom[0][0];

		do
		{
			////////////////////////////////////////////////////
			// process one iteration of projection

			// add shaping
			view_x2 += view_dx;
			view_y2 += view_dy;

			// vertical scroll calculation
			view_xofs2 = view_x2;
			view_yofs2 = (short) ((world_yofs * distance >> 15) + poly_bottom[0][0] - view_y2);

			// 1. Viewer x-position at the next
			// 2. Viewer y-position below the horizon
			// 3. Number of raster lines drawn in this iteration

			DSP4_CLEAR_OUT();
			DSP4_WRITE_WORD(view_x2);
			DSP4_WRITE_WORD(view_y2);

			//////////////////////////////////////////////////////

			// SR = 0x00

			// determine # of raster lines used
			segments = (short) (view_y1 - view_y2);

			// prevent overdraw
			if (view_y2 >= poly_raster[0][0])
				segments = 0;
			else
				poly_raster[0][0] = view_y2;

			// don't draw outside the window
			if (view_y2 < poly_top[0][0])
			{
				segments = 0;

				// flush remaining raster lines
				if (view_y1 >= poly_top[0][0])
					segments = (short) (view_y1 - poly_top[0][0]);
			}

			// SR = 0x80

			DSP4_WRITE_WORD(segments);

			//////////////////////////////////////////////////////

			// scan next command if no SR check needed
			if (segments > 0)
			{
				for (lcv = 0; lcv < 4; lcv++)
				{
					// grab inputs
					in_count = 4;
					DSP4_WAIT(1);
					resume1 :
					for (;;)
					{
						short distance;
						short color, red, green, blue;

						distance = DSP4_READ_WORD();
						color = DSP4_READ_WORD();

						// U1+B5+G5+R5
						red = (short) (color & 0x1f);
						green = (short) ((color >> 5) & 0x1f);
						blue = (short) ((color >> 10) & 0x1f);

						// dynamic lighting
						red = (short) ((red * distance >> 15) & 0x1f);
						green = (short) ((green * distance >> 15) & 0x1f);
						blue = (short) ((blue * distance >> 15) & 0x1f);
						color = (short) (red | (green << 5) | (blue << 10));

						DSP4_CLEAR_OUT();
						DSP4_WRITE_WORD(color);
						break;
					}
				}
			}

			//////////////////////////////////////////////////////

			// scan next command if no SR check needed
			if (segments > 0)
			{
				int px_dx, py_dy;
				int x_scroll, y_scroll;

				// SR = 0x00

				// linear interpolation (lerp) between projected points
				px_dx = (view_xofs2 - view_xofs1) * DSP4_Inverse(segments) << 1;
				py_dy = (view_yofs2 - view_yofs1) * DSP4_Inverse(segments) << 1;

				// starting step values
				x_scroll = SEX16(poly_cx[0][0] + view_xofs1);
				y_scroll = SEX16(-viewport_bottom + view_yofs1 + view_yofsenv + poly_cx[1][0] - world_yofs);

				// SR = 0x80

				// rasterize line
				for (lcv = 0; lcv < segments; lcv++)
				{
					// 1. HDMA memory pointer (bg2)
					// 2. vertical scroll offset ($2110)
					// 3. horizontal scroll offset ($210F)

					DSP4_WRITE_WORD(poly_ptr[0][0]);
					DSP4_WRITE_WORD((y_scroll + 0x8000) >> 16);
					DSP4_WRITE_WORD((x_scroll + 0x8000) >> 16);

					// update memory address
					poly_ptr[0][0] -= 4;

					// update screen values
					x_scroll += px_dx;
					y_scroll += py_dy;
				}
			}

			/////////////////////////////////////////////////////
			// Post-update

			// update new viewer (x,y,scroll) to last raster line drawn
			view_x1 = view_x2;
			view_y1 = view_y2;
			view_xofs1 = view_xofs2;
			view_yofs1 = view_yofs2;

			////////////////////////////////////////////////////
			// command check

			// scan next command
			in_count = 2;
			DSP4_WAIT((short) 2);
			resume2 :

			// check for opcode termination
			distance = DSP4_READ_WORD();
			if (distance == -0x8000)
				break;

			// already have 2 bytes in queue
			in_count = 10;
			DSP4_WAIT((short) 3);
			resume3 :


			// inspect inputs
			view_y2 = DSP4_READ_WORD();
			view_dy = (short) (DSP4_READ_WORD() * distance >> 15);
			view_x2 = DSP4_READ_WORD();
			view_dx = (short) (DSP4_READ_WORD() * distance >> 15);
		}
		while ( true );

		waiting4command = true;
	}

	private void DSP4_OP08()
	{
		short win_left, win_right;
		short view_x[] = new short[2];
		short view_y[] = new short[2];
		short envelope[][] = new short[2][2];

		waiting4command = false;

		// op flow control
		switch (DSP4_Logic)
		{
			case 1:
				//goto resume1;
				break;
			case 2:
				//goto resume2;
				break;
		}

		////////////////////////////////////////////////////
		// process initial inputs for two polygons

		// clip values
		poly_clipRt[0][0] = DSP4_READ_WORD();
		poly_clipRt[0][1] = DSP4_READ_WORD();
		poly_clipRt[1][0] = DSP4_READ_WORD();
		poly_clipRt[1][1] = DSP4_READ_WORD();

		poly_clipLf[0][0] = DSP4_READ_WORD();
		poly_clipLf[0][1] = DSP4_READ_WORD();
		poly_clipLf[1][0] = DSP4_READ_WORD();
		poly_clipLf[1][1] = DSP4_READ_WORD();

		// unknown (constant) (ex. 1P/2P = $00A6, $00A6, $00A6, $00A6)
		DSP4_READ_WORD();
		DSP4_READ_WORD();
		DSP4_READ_WORD();
		DSP4_READ_WORD();

		// unknown (constant) (ex. 1P/2P = $00A5, $00A5, $00A7, $00A7)
		DSP4_READ_WORD();
		DSP4_READ_WORD();
		DSP4_READ_WORD();
		DSP4_READ_WORD();

		// polygon centering (left,right)
		poly_cx[0][0] = DSP4_READ_WORD();
		poly_cx[0][1] = DSP4_READ_WORD();
		poly_cx[1][0] = DSP4_READ_WORD();
		poly_cx[1][1] = DSP4_READ_WORD();

		// HDMA pointer locations
		poly_ptr[0][0] = DSP4_READ_WORD();
		poly_ptr[0][1] = DSP4_READ_WORD();
		poly_ptr[1][0] = DSP4_READ_WORD();
		poly_ptr[1][1] = DSP4_READ_WORD();

		// starting raster line below the horizon
		poly_bottom[0][0] = DSP4_READ_WORD();
		poly_bottom[0][1] = DSP4_READ_WORD();
		poly_bottom[1][0] = DSP4_READ_WORD();
		poly_bottom[1][1] = DSP4_READ_WORD();

		// top boundary line to clip
		poly_top[0][0] = DSP4_READ_WORD();
		poly_top[0][1] = DSP4_READ_WORD();
		poly_top[1][0] = DSP4_READ_WORD();
		poly_top[1][1] = DSP4_READ_WORD();

		// unknown
		// (ex. 1P = $2FC8, $0034, $FF5C, $0035)
		//
		// (ex. 2P = $3178, $0034, $FFCC, $0035)
		// (ex. 2P = $2FC8, $0034, $FFCC, $0035)

		DSP4_READ_WORD();
		DSP4_READ_WORD();
		DSP4_READ_WORD();
		DSP4_READ_WORD();

		// look at guidelines for both polygon shapes
		distance = DSP4_READ_WORD();
		view_x[0] = DSP4_READ_WORD();
		view_y[0] = DSP4_READ_WORD();
		view_x[1] = DSP4_READ_WORD();
		view_y[1] = DSP4_READ_WORD();

		// envelope shaping guidelines (one frame only)
		envelope[0][0] = DSP4_READ_WORD();
		envelope[0][1] = DSP4_READ_WORD();
		envelope[1][0] = DSP4_READ_WORD();
		envelope[1][1] = DSP4_READ_WORD();

		// starting base values to project from
		poly_start[0] = view_x[0];
		poly_start[1] = view_x[1];

		// starting raster lines to begin drawing
		poly_raster[0][0] = view_y[0];
		poly_raster[0][1] = view_y[0];
		poly_raster[1][0] = view_y[1];
		poly_raster[1][1] = view_y[1];

		// starting distances
		poly_plane[0] = distance;
		poly_plane[1] = distance;

		// SR = 0x00

		// re-center coordinates
		win_left = (short) (poly_cx[0][0] - view_x[0] + envelope[0][0]);
		win_right = (short) (poly_cx[0][1] - view_x[0] + envelope[0][1]);

		// saturate offscreen data for polygon #1
		if (win_left < poly_clipLf[0][0])
		{
			win_left = poly_clipLf[0][0];
		}
		if (win_left > poly_clipRt[0][0])
		{
			win_left = poly_clipRt[0][0];
		}
		if (win_right < poly_clipLf[0][1])
		{
			win_right = poly_clipLf[0][1];
		}
		if (win_right > poly_clipRt[0][1])
		{
			win_right = poly_clipRt[0][1];
		}

		// SR = 0x80

		// initial output for polygon #1
		DSP4_CLEAR_OUT();
		DSP4_WRITE_BYTE(win_left & 0xff);
		DSP4_WRITE_BYTE(win_right & 0xff);


		do
		{
			short polygon;
			////////////////////////////////////////////////////
			// command check

			// scan next command
			in_count = 2;
			DSP4_WAIT(1);
			
			resume1 :

			// terminate op
			distance = DSP4_READ_WORD();
			if (distance == -0x8000)
				break;

			// already have 2 bytes in queue
			in_count = 16;

			DSP4_WAIT(2);
			
			resume2 :

			// look at guidelines for both polygon shapes
			view_x[0] = DSP4_READ_WORD();
			view_y[0] = DSP4_READ_WORD();
			view_x[1] = DSP4_READ_WORD();
			view_y[1] = DSP4_READ_WORD();

			// envelope shaping guidelines (one frame only)
			envelope[0][0] = DSP4_READ_WORD();
			envelope[0][1] = DSP4_READ_WORD();
			envelope[1][0] = DSP4_READ_WORD();
			envelope[1][1] = DSP4_READ_WORD();

			////////////////////////////////////////////////////
			// projection begins

			// init
			DSP4_CLEAR_OUT();


			//////////////////////////////////////////////
			// solid polygon renderer - 2 shapes

			for (polygon = 0; polygon < 2; polygon++)
			{
				int left_inc, right_inc;
				short x1_final, x2_final;
				short env[][] = new short[2][2];
				short poly;

				// SR = 0x00

				// # raster lines to draw
				segments = (short) (poly_raster[polygon][0] - view_y[polygon]);

				// prevent overdraw
				if (segments > 0)
				{
					// bump drawing cursor
					poly_raster[polygon][0] = view_y[polygon];
					poly_raster[polygon][1] = view_y[polygon];
				}
				else
					segments = 0;

				// don't draw outside the window
				if (view_y[polygon] < poly_top[polygon][0])
				{
					segments = 0;

					// flush remaining raster lines
					if (view_y[polygon] >= poly_top[polygon][0])
						segments = (short) (view_y[polygon] - poly_top[polygon][0]);
				}

				// SR = 0x80

				// tell user how many raster structures to read in
				DSP4_WRITE_WORD(segments);

				// normal parameters
				poly = polygon;

				/////////////////////////////////////////////////////

				// scan next command if no SR check needed
				if (segments > 0 )
				{
					int iwin_left, iwin_right;

					// road turnoff selection
					if( envelope[ polygon ][ 0 ] == 0xc001 )
						poly = 1;
					else if( envelope[ polygon ][ 1 ] == 0x3fff )
						poly = 1;

					///////////////////////////////////////////////
					// left side of polygon

					// perspective correction on additional shaping parameters
					env[0][0] = (short) (envelope[polygon][0] * poly_plane[poly] >> 15);
					env[0][1] = (short) (envelope[polygon][0] * distance >> 15);

					// project new shapes (left side)
					x1_final = (short) (view_x[poly] + env[0][0]);
					x2_final = (short) (poly_start[poly] + env[0][1]);

					// interpolate between projected points with shaping
					left_inc = (x2_final - x1_final) * DSP4_Inverse(segments) << 1;
					if (segments == 1)
						left_inc = -left_inc;

					///////////////////////////////////////////////
					// right side of polygon

					// perspective correction on additional shaping parameters
					env[1][0] = (short) (envelope[polygon][1] * poly_plane[poly] >> 15);;
					env[1][1] = (short) (envelope[polygon][1] * distance >> 15);

					// project new shapes (right side)
					x1_final = (short) (view_x[poly] + env[1][0]);
					x2_final = (short) (poly_start[poly] + env[1][1]);


					// interpolate between projected points with shaping
					right_inc = (x2_final - x1_final) * DSP4_Inverse(segments) << 1;
					if (segments == 1)
						right_inc = -right_inc;

					///////////////////////////////////////////////
					// update each point on the line

					iwin_left = SEX16(poly_cx[polygon][0] - poly_start[poly] + env[0][0]);
					iwin_right = SEX16(poly_cx[polygon][1] - poly_start[poly] + env[1][0]);

					// update distance drawn into world
					poly_plane[polygon] = distance;

					// rasterize line
					for (lcv = 0; lcv < segments; lcv++)
					{
						short x_left, x_right;

						// project new coordinates
						iwin_left += left_inc;
						iwin_right += right_inc;

						// grab integer portion, drop fraction (no rounding)
						x_left = (short) (iwin_left >> 16);
						x_right = (short) (iwin_right >> 16);

						// saturate offscreen data
						if (x_left < poly_clipLf[polygon][0])
							x_left = poly_clipLf[polygon][0];
						if (x_left > poly_clipRt[polygon][0])
							x_left = poly_clipRt[polygon][0];
						if (x_right < poly_clipLf[polygon][1])
							x_right = poly_clipLf[polygon][1];
						if (x_right > poly_clipRt[polygon][1])
							x_right = poly_clipRt[polygon][1];

						// 1. HDMA memory pointer
						// 2. Left window position ($2126/$2128)
						// 3. Right window position ($2127/$2129)

						DSP4_WRITE_WORD(poly_ptr[polygon][0]);
						DSP4_WRITE_BYTE(x_left & 0xff);
						DSP4_WRITE_BYTE(x_right & 0xff);


						// update memory pointers
						poly_ptr[polygon][0] -= 4;
						poly_ptr[polygon][1] -= 4;
					} // end rasterize line
				}

				////////////////////////////////////////////////
				// Post-update

				// new projection spot to continue rasterizing from
				poly_start[polygon] = view_x[poly];
			} // end polygon rasterizer
		}
		while ( true );

		// unknown output
		DSP4_CLEAR_OUT();
		DSP4_WRITE_WORD(0);


		waiting4command = true;
	}

	//////////////////////////////////////////////////////////////

	void DSP4_OP09()
	{
		waiting4command = false;

		// op flow control
		switch (DSP4_Logic)
		{
			// TODO: Figure out how to do goto's
			case 1:
				//goto resume1;
				break;
			case 2:
				//goto resume2;
				break;
			case 3:
				//goto resume3;
				break;
			case 4:
				//goto resume4;
				break;
			case 5:
				//goto resume5;
				break;
			case 6:
				//goto resume6;
				break;
		}

		////////////////////////////////////////////////////
		// process initial inputs

		// grab screen information
		viewport_cx = DSP4_READ_WORD();
		viewport_cy = DSP4_READ_WORD();
		DSP4_READ_WORD(); // 0x0000
		viewport_left = DSP4_READ_WORD();
		viewport_right = DSP4_READ_WORD();
		viewport_top = DSP4_READ_WORD();
		viewport_bottom = DSP4_READ_WORD();

		// starting raster line below the horizon
		poly_bottom[0][0] = (short) (viewport_bottom - viewport_cy);
		poly_raster[0][0] = 0x100;

		// TODO: Figure out how to do goto's
		terminate:
			
		do
		{
			////////////////////////////////////////////////////
			// check for new sprites

			in_count = 4;
			DSP4_WAIT(1);
			
			resume1:

			////////////////////////////////////////////////
			// raster overdraw check

			raster = DSP4_READ_WORD();

			// continue updating the raster line where overdraw begins
			if (raster < poly_raster[0][0])
			{
				sprite_clipy = (short) (viewport_bottom - (poly_bottom[0][0] - raster));
				poly_raster[0][0] = raster;
			}

			/////////////////////////////////////////////////
			// identify sprite

			// op termination
			distance = DSP4_READ_WORD();
			if (distance == -0x8000)
			{
				// TODO: Figure out how to do goto's
				//goto terminate;
			}


			// no sprite
			if (distance == 0x0000)
			{
				continue;
			}

			////////////////////////////////////////////////////
			// process projection information
				
			// vehicle sprite
			if ( distance == 0x9000)
			{
				short car_left, car_right, car_back;
				short impact_left, impact_back;
				short world_spx, world_spy;
				short view_spx, view_spy;
				int energy;

				// we already have 4 bytes we want
				in_count = 14;
				DSP4_WAIT(2);
				
				resume2 :

				// filter inputs
				energy = DSP4_READ_WORD();
				impact_back = DSP4_READ_WORD();
				car_back = DSP4_READ_WORD();
				impact_left = DSP4_READ_WORD();
				car_left = DSP4_READ_WORD();
				distance = DSP4_READ_WORD();
				car_right = DSP4_READ_WORD();

				// calculate car's world (x,y) values
				world_spx = (short) (car_right - car_left);
				world_spy = car_back;

				// add in collision vector [needs bit-twiddling]
				world_spx -= energy * (impact_left - car_left) >> 16;
				world_spy -= energy * (car_back - impact_back) >> 16;

				// perspective correction for world (x,y)
				view_spx = (short) (world_spx * distance >> 15);
				view_spy = (short) (world_spy * distance >> 15);

				// convert to screen values
				sprite_x = (short) (viewport_cx + view_spx);
				sprite_y = (short) (viewport_bottom - (poly_bottom[0][0] - view_spy));

				// make the car's (x)-coordinate available
				DSP4_CLEAR_OUT();
				DSP4_WRITE_WORD(world_spx);

				// grab a few remaining vehicle values
				in_count = 4;
				DSP4_WAIT(3);
				
				resume3 :

				// add vertical lift factor
				sprite_y += DSP4_READ_WORD();
			}
			// terrain sprite
			else
			{
				short world_spx, world_spy;
				short view_spx, view_spy;

				// we already have 4 bytes we want
				in_count = 10;
				DSP4_WAIT(4);
				
				resume4 :

				// sort loop inputs
				poly_cx[0][0] = DSP4_READ_WORD();
				poly_raster[0][1] = DSP4_READ_WORD();
				world_spx = DSP4_READ_WORD();
				world_spy = DSP4_READ_WORD();

				// compute base raster line from the bottom
				segments = (short) (poly_bottom[0][0] - raster);

				// perspective correction for world (x,y)
				view_spx = (short) (world_spx * distance >> 15);
				view_spy = (short) (world_spy * distance >> 15);

				// convert to screen values
				sprite_x = (short) (viewport_cx + view_spx - poly_cx[0][0]);
				sprite_y = (short) (viewport_bottom - segments + view_spy);
			}

			// default sprite size: 16x16
			sprite_size = true;
			sprite_attr = DSP4_READ_WORD();

			////////////////////////////////////////////////////
			// convert tile data to SNES OAM format

			do
			{
				int header;

				short sp_x, sp_y, sp_attr, sp_dattr;
				short sp_dx, sp_dy;
				short pixels;

				boolean draw;

				in_count = 2;
				DSP4_WAIT(5);
				
				resume5:

				draw = true;

				// opcode termination
				raster = DSP4_READ_WORD();
				if (raster == -0x8000)
				{
					break terminate;
				}

				// stop code
				if (raster == 0x0000 && !sprite_size)
					break;

				// toggle sprite size
				if (raster == 0x0000)
				{
					sprite_size = !sprite_size;
					continue;
				}

				// check for valid sprite header
				header = raster;
				header >>= 8;
				if (header != 0x20 &&
						header != 0x2e && //This is for attractor sprite
						header != 0x40 &&
						header != 0x60 &&
						header != 0xa0 &&
						header != 0xc0 &&
						header != 0xe0)
					break;

				// read in rest of sprite data
				in_count = 4;
				DSP4_WAIT(6);
				
				resume6:

				draw = true;

				/////////////////////////////////////
				// process tile data

				// sprite deltas
				sp_dattr = raster;
				sp_dy = DSP4_READ_WORD();
				sp_dx = DSP4_READ_WORD();

				// update coordinates to screen space
				sp_x = (short) (sprite_x + sp_dx);
				sp_y = (short) (sprite_y + sp_dy);

				// update sprite nametable/attribute information
				sp_attr = (short) (sprite_attr + sp_dattr);

				// allow partially visibile tiles
				pixels = (short) (sprite_size ? 15 : 7);

				DSP4_CLEAR_OUT();

				// transparent tile to clip off parts of a sprite (overdraw)
				if (sprite_clipy - pixels <= sp_y &&
						sp_y <= sprite_clipy &&
						sp_x >= viewport_left - pixels &&
						sp_x <= viewport_right &&
						sprite_clipy >= viewport_top - pixels &&
						sprite_clipy <= viewport_bottom)
				{
					draw = DSP4_OP0B(draw, sp_x, sprite_clipy, (short) 0x00EE, sprite_size, false);
				}


				// normal sprite tile
				if (sp_x >= viewport_left - pixels &&
						sp_x <= viewport_right &&
						sp_y >= viewport_top - pixels &&
						sp_y <= viewport_bottom &&
						sp_y <= sprite_clipy)
				{
					draw = DSP4_OP0B(draw, sp_x, sp_y, sp_attr, sprite_size, false);
				}

				// no following OAM data
				draw = DSP4_OP0B( draw, (short) 0, (short) 0x0100, (short) 0, false, true);
			}
			while ( true );
		}
		while ( true );


		
		waiting4command = true;
	}
	
	private int DSP4_OP11(int A, int B, int C, int D)
	{
		// 0x155 = 341 = Horizontal Width of the Screen
		return	
			(((A * 0x0155 >> 2) & 0xf000) |
		((B * 0x0155 >> 6) & 0x0f00) |
		((C * 0x0155 >> 10) & 0x00f0) |
		((D * 0x0155 >> 14) & 0x000f));
	}
	
	private boolean DSP4_OP0B(boolean draw, short sp_x, short sp_y, short sp_attr, boolean size, boolean stop)
	{
		short Row1, Row2;

		// SR = 0x00

		// align to nearest 8-pixel row
		Row1 = (short) ((sp_y >> 3) & 0x1f);
		Row2 = (short) ((Row1 + 1) & 0x1f);

		// check boundaries
		if (!((sp_y < 0) || ((sp_y & 0x01ff) < 0x00eb)))
		{
			draw = false;
		}
		if (size)
		{
			if (OAM_Row[Row1] + 1 >= OAM_RowMax)
				draw = false;
			if (OAM_Row[Row2] + 1 >= OAM_RowMax)
				draw = false;
		}
		else
		{
			if (OAM_Row[Row1] >= OAM_RowMax)
			{
				draw = false;
			}
		}

		// emulator fail-safe (unknown if this really exists)
		if (sprite_count >= 128)
		{
			draw = false;
		}

		// SR = 0x80

		if (draw)
		{
			// Row tiles
			if (size)
			{
				OAM_Row[Row1] += 2;
				OAM_Row[Row2] += 2;
			}
			else
			{
				OAM_Row[Row1]++;
			}

			// yield OAM output
			DSP4_WRITE_WORD(1);

			// pack OAM data: x,y,name,attr
			DSP4_WRITE_BYTE(sp_x & 0xff);
			DSP4_WRITE_BYTE(sp_y & 0xff);
			DSP4_WRITE_WORD(sp_attr);

			sprite_count++;

			// OAM: size,msb data
			// save post-oam table data for future retrieval
			OAM_attr[OAM_index] |= ( ( (sp_x < 0 || sp_x > 255) ? 1 : 0 ) << OAM_bits
					);
			OAM_bits++;

			OAM_attr[OAM_index] |= (( size ? 1 : 0) << OAM_bits);
			OAM_bits++;

			// move to next byte in buffer
			if (OAM_bits == 16)
			{
				OAM_bits = 0;
				OAM_index++;
			}
		}
		else if (stop)
		{
			// yield no OAM output
			DSP4_WRITE_WORD(0);
		}
		
		return draw;
	}
	
	void DSP4_OP0D()
	{
		waiting4command = false;

		// op flow control
		switch (DSP4_Logic)
		{
			case 1:
				// TODO: Figure out how to do goto's					
				//goto resume1;
				break;
			case 2:
				//goto resume2;
				break;
		}

		////////////////////////////////////////////////////
		// process initial inputs

		// sort inputs
		world_y = DSP4_READ_DWORD();
		poly_bottom[0][0] = DSP4_READ_WORD();
		poly_top[0][0] = DSP4_READ_WORD();
		poly_cx[1][0] = DSP4_READ_WORD();
		viewport_bottom = DSP4_READ_WORD();
		world_x = DSP4_READ_DWORD();
		poly_cx[0][0] = DSP4_READ_WORD();
		poly_ptr[0][0] = DSP4_READ_WORD();
		world_yofs = DSP4_READ_WORD();
		world_dy = DSP4_READ_DWORD();
		world_dx = DSP4_READ_DWORD();
		distance = DSP4_READ_WORD();
		DSP4_READ_WORD(); // 0x0000
		world_xenv = SEX78(DSP4_READ_WORD());
		world_ddy = DSP4_READ_WORD();
		world_ddx = DSP4_READ_WORD();
		view_yofsenv = DSP4_READ_WORD();

		// initial (x,y,offset) at starting raster line
		view_x1 = (short) ((world_x + world_xenv) >> 16);
		view_y1 = (short) (world_y >> 16);
		view_xofs1 = (short) (world_x >> 16);
		view_yofs1 = world_yofs;

		// first raster line
		poly_raster[0][0] = poly_bottom[0][0];


		do
		{
			////////////////////////////////////////////////////
			// process one iteration of projection

			// perspective projection of world (x,y,scroll) points
			// based on the current projection lines
			view_x2 = (short) (( ( ( world_x + world_xenv ) >> 16 ) * distance >> 15 ) + ( view_turnoff_x * distance >> 15 ));
			view_y2 = (short) ((world_y >> 16) * distance >> 15);
			view_xofs2 = view_x2;
			view_yofs2 = (short) ((world_yofs * distance >> 15) + poly_bottom[0][0] - view_y2);

			// 1. World x-location before transformation
			// 2. Viewer x-position at the current
			// 3. World y-location before perspective projection
			// 4. Viewer y-position below the horizon
			// 5. Number of raster lines drawn in this iteration

			DSP4_CLEAR_OUT();
			DSP4_WRITE_WORD((world_x + world_xenv) >> 16);
			DSP4_WRITE_WORD(view_x2);
			DSP4_WRITE_WORD(world_y >> 16);
			DSP4_WRITE_WORD(view_y2);

			//////////////////////////////////////////////////////////

			// SR = 0x00

			// determine # of raster lines used
			segments = (short) (view_y1 - view_y2);

			// prevent overdraw
			if (view_y2 >= poly_raster[0][0])
				segments = 0;
			else
				poly_raster[0][0] = view_y2;

			// don't draw outside the window
			if (view_y2 < poly_top[0][0])
			{
				segments = 0;

				// flush remaining raster lines
				if (view_y1 >= poly_top[0][0])
					segments = (short) (view_y1 - poly_top[0][0]);
			}

			// SR = 0x80

			DSP4_WRITE_WORD(segments);

			//////////////////////////////////////////////////////////

			// scan next command if no SR check needed
			if (segments > 0)
			{
				int px_dx, py_dy;
				int x_scroll, y_scroll;

				// SR = 0x00

				// linear interpolation (lerp) between projected points
				px_dx = (view_xofs2 - view_xofs1) * DSP4_Inverse(segments) << 1;
				py_dy = (view_yofs2 - view_yofs1) * DSP4_Inverse(segments) << 1;

				// starting step values
				x_scroll = SEX16(poly_cx[0][0] + view_xofs1);
				y_scroll = SEX16(-viewport_bottom + view_yofs1 + view_yofsenv + poly_cx[1][0] - world_yofs);

				// SR = 0x80

				// rasterize line
				for (lcv = 0; lcv < segments; lcv++)
				{
					// 1. HDMA memory pointer (bg1)
					// 2. vertical scroll offset ($210E)
					// 3. horizontal scroll offset ($210D)

					DSP4_WRITE_WORD(poly_ptr[0][0]);
					DSP4_WRITE_WORD((y_scroll + 0x8000) >> 16);
					DSP4_WRITE_WORD((x_scroll + 0x8000) >> 16);


					// update memory address
					poly_ptr[0][0] -= 4;

					// update screen values
					x_scroll += px_dx;
					y_scroll += py_dy;
				}
			}

			/////////////////////////////////////////////////////
			// Post-update

			// update new viewer (x,y,scroll) to last raster line drawn
			view_x1 = view_x2;
			view_y1 = view_y2;
			view_xofs1 = view_xofs2;
			view_yofs1 = view_yofs2;

			// add deltas for projection lines
			world_dx += SEX78(world_ddx);
			world_dy += SEX78(world_ddy);

			// update projection lines
			world_x += (world_dx + world_xenv);
			world_y += world_dy;

			////////////////////////////////////////////////////
			// command check

			// scan next command
			in_count = 2;
			DSP4_WAIT(1);
			
			resume1:

			// inspect input
			distance = DSP4_READ_WORD();

			// terminate op
			if (distance == -0x8000)
				break;

			// already have 2 bytes in queue
			in_count = 6;
			DSP4_WAIT(2);
			
			resume2:

			// inspect inputs
			world_ddy = DSP4_READ_WORD();
			world_ddx = DSP4_READ_WORD();
			view_yofsenv = DSP4_READ_WORD();

			// no envelope here
			world_xenv = 0;
		}
		while ( true);

		waiting4command = true;
	}
	
	private void DSP4_CLEAR_OUT()
	{
		out_count = 0;
		out_index = 0;
	}
	
	// used to wait for dsp i/o
	private void DSP4_WAIT( int x )
	{
		in_index = 0;
		DSP4_Logic = x;
		return;
	}

	// 1.7.8 -> 1.15.16
	private int SEX78( int a )
	{
		return ( ( (int) ( (short) (a) ) ) << 8 );
	}

	// 1.15.0 -> 1.15.16
	private int SEX16( int i )
	{
		return ( ( (int) ( (short) (i) ) ) << 16 );
	}
	
	private short DSP4_READ_WORD()
	{
		short out = (short) parameters.get16Bit(in_index);
		in_index += 2;
		return out;
	}

	private int DSP4_READ_DWORD()
	{
		int out;

		out = parameters.get32Bit(in_index);
		in_index += 4;

		return out;
	}

	private void DSP4_WRITE_BYTE( int d )
	{
		output.put8Bit(out_count, d);
		out_count++;
	}
	
	private void	DSP4_WRITE_WORD( int d )
	{
		output.put16Bit(out_count, d ); 
		out_count += 2;
	}

	private void	 DSP4_WRITE_16_WORD( short[] d )
	{
		//NAC: memcpy(DSP4.output + DSP4.out_count, ( d ), 32);
		
		for( int i = 0; i < 16; i++)
			output.put16Bit(i << 1, d[i]);
		
		out_count += 32;
	}

	private int DSP4_Multiply(int Multiplicand, int Multiplier )
	{
		return (Multiplicand * Multiplier << 1) >> 1;
	}
}

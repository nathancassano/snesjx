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

class Controls
{
	private static Globals globals;
	private static Memory Memory;
	
	static final int CTL_NONE = 0;
	static final int CTL_JOYPAD = 1;
	static final int CTL_MOUSE = 2;
    
	private static final int NONE       = -2;
	private static final int JOYPAD0    =  0;
	private static final int JOYPAD1    =  1;
	private static final int MOUSE0     =  8;
	private static final int MOUSE1     =  9;
	private static final int NUMCTLS    = 10;
	
	private int turbo_time = 0;
	private boolean pad_read = false;
	private boolean pad_read_last = false;
	
	private static int curcontrollers[] = { JOYPAD0, JOYPAD0 };
	private static int newcontrollers[] = { JOYPAD0, JOYPAD0 };
	
	private int read_idx[][] = new int[2][2];
	
	private static boolean FLAG_LATCH = false;
	
	private class Joypad
	{
		private int buttons;
		private int turbos;
		private int toggleturbo;
		private int togglestick;
		private int turbo_ct;
	}
	
	private class Crosshair
	{
		private int set;
		private int img;
		private int fg;
		private int bg;
	}
	
	private class Mouse
	{	
		private int delta_x, delta_y;
		private int old_x, old_y;
		private int cur_x, cur_y;
		private int buttons;
		private int ID;
		
		private Crosshair crosshair = new Crosshair();
	}
	
	private Joypad joypad[] = new Joypad[8];
	
	private Mouse mouse[] = new Mouse[2];
	
	Controls()
	{
		for(int i = 0; i < joypad.length; i++ )
			joypad[i] = new Joypad();

		for(int i = 0; i < mouse.length; i++ )
			mouse[i] = new Mouse();

	}
	
	void setUp()
	{
		globals = Globals.globals;
		Memory = globals.memory;
	}
	
	void ControlsReset()
	{
	    ControlsSoftReset();
	    mouse[0].buttons&=~0x30;
	    mouse[1].buttons&=~0x30;
	}

	void ControlsSoftReset()
	{
	    int i, j;

	    for( i = 0; i < 2; i++)
	    {
	        for( j = 0; j < 2; j++)
	        {
	            read_idx[i][j] = 0;
	        }
	    }

	    FLAG_LATCH = false;
	}
	
	void SetJoypadLatch(boolean latch)
	{
	    if(!latch && FLAG_LATCH){
	        // 1 written, 'plug in' new controllers now
	        curcontrollers[0]=newcontrollers[0];
	        curcontrollers[1]=newcontrollers[1];
	    }
	    if( latch && !FLAG_LATCH )
	    {
	        int i, j, n;

	        for(n = 0; n < 2; n++){
	            for(j=0; j<2; j++){
	                read_idx[n][j]=0;
	            }
	            switch(i=curcontrollers[n])
	            {
	              case JOYPAD0: case JOYPAD1:
	                do_polling(i);
	                break;

	              case MOUSE0: case MOUSE1:
	                do_polling(i);
	                break;

	              default:
	                break;
	            }
	        }
	    }
	    FLAG_LATCH=latch;
	}
	
	int ReadJOYSERn(int n)
	{
		int i;

		if( n > 1 )
		{
			n -= 0x4016;
		}

		int bits = ( globals.OpenBus & ~3 ) | ( ( n == 1 ) ? 0x1c : 0 );

		if( FLAG_LATCH )
		{
			i = curcontrollers[n];
			switch( i )
			{
			case JOYPAD0: case JOYPAD1:
				return bits | ( ( joypad[ i - JOYPAD0].buttons & 0x8000 ) >> 15 );
			case MOUSE0: case MOUSE1:
				mouse[i-MOUSE0].buttons += 0x10;
				
				if ( (mouse[i-MOUSE0].buttons & 0x30 ) != 0 )
				{
					mouse[i-MOUSE0].buttons &= 0xcf;
				}
				return bits;
			default:
				return bits;
			}
		} else {
			switch(i = curcontrollers[n])
			{
			case JOYPAD0:
			case JOYPAD1: 
				if(read_idx[n][0]>=16){
					read_idx[n][0]++;
					return bits | 1;
				} else {
					return bits | ( ( joypad[i-JOYPAD0].buttons & ( 0x8000 >> read_idx[n][0]++ ) ) != 0 ? 1 : 0);
				}
			case MOUSE0: case MOUSE1:
				if(read_idx[n][0] < 8 )
				{
					read_idx[n][0]++;
					return bits;
				}
				else if(read_idx[n][0] < 16)
				{
					return bits | ( ( mouse[i-MOUSE0].buttons & ( 0x8000 >> read_idx[n][0]++ ) ) != 0 ? 1 : 0);
				}
				else if(read_idx[n][0] < 24)
				{
					return bits | ( ( mouse[i-MOUSE0].delta_y & ( 0x800000 >> read_idx[n][0]++ ) ) != 0 ? 1 : 0);
				}
				else if(read_idx[n][0] < 32)
				{
					return bits | ( ( mouse[i-MOUSE0].delta_x & ( 0x80000000 >> read_idx[n][0]++ ) ) != 0 ? 1 : 0);
				}
				else
				{
					read_idx[n][0]++;
					return bits|1;
				}
			default:
				read_idx[n][0]++;
				return bits;
			}
		}
	}
	
	void DoAutoJoypad()
	{
	    int n, i;

	    SetJoypadLatch(true);
	    SetJoypadLatch(false);
		//MovieUpdate(false);

	    for( n = 0; n < 2; n++)
	    {
	        switch( i = curcontrollers[n])
	        {
	          case JOYPAD0: case JOYPAD1:
	            read_idx[n][0]=16;
	            Memory.FillRAM.put16Bit(0x4218 + n * 2, joypad[i-JOYPAD0].buttons);
	            Memory.FillRAM.put16Bit(0x421c + n * 2, 0);
	            break;
	          case MOUSE0: case MOUSE1:
	            read_idx[n][0]=16;
	            Memory.FillRAM.put16Bit(0x4218 + n * 2, mouse[i-MOUSE0].buttons);
	            Memory.FillRAM.put16Bit(0x421c + n * 2, 0);
	            break;
	          default:
		        Memory.FillRAM.put16Bit(0x4218 + n * 2, 0);
	            Memory.FillRAM.put16Bit(0x421c + n * 2, 0);
	            break;
	        }
	    }
	}
	
	void VerifyControllers()
	{
		//NAC: Don't think this is necessary 
		/*
	    boolean ret = false;
	    int port;
	    int i;
	    int used[] = new int[NUMCTLS];

	    for(i = 0; i < NUMCTLS; used[i++] = 0);

	    for( port = 0; port < 2; port++)
	    {
	        switch( i = newcontrollers[port])
	        {
	          case MOUSE0: case MOUSE1:
	            if(!Settings.MouseMaster){
	                Message(_CONFIG_INFO, _ERROR, "Cannot select SNES Mouse: MouseMaster disabled");
	                newcontrollers[port]=NONE;
	                ret=true;
	                break;
	            }
	            if(used[i]++>0){
	                snprintf(buf, sizeof(buf), "Mouse%d used more than once! Disabling extra instances", i-MOUSE0+1);
	                Message(_CONFIG_INFO, _ERROR, buf);
	                newcontrollers[port]=NONE;
	                ret=true;
	                break;
	            }
	            break;
	          case JOYPAD0:
	          case JOYPAD1:
	            if(used[i-JOYPAD0]++>0){
	                snprintf(buf, sizeof(buf), "Joypad%d used more than once! Disabling extra instances", i-JOYPAD0+1);
	                Message(_CONFIG_INFO, _ERROR, buf);
	                newcontrollers[port]=NONE;
	                ret=true;
	                break;
	            }
	            break;
	          default:
	            break;
	        }
	    }

	    return ret;
	    */
	}
	
	private void ControlEOF()
	{
	    int i, j, n;
	    Crosshair c;

	    //globals.ppu.GunVLatch = 1000; /* i.e., never latch */
	    //globals.ppu.GunHLatch = 0;

	    for( n = 0; n < 2; n++ )
	    {
	        switch( i = curcontrollers[n])
	        {
	          case JOYPAD0: case JOYPAD1:
	            if(++joypad[i-JOYPAD0].turbo_ct >= turbo_time)
	            {
	                joypad[i-JOYPAD0].turbo_ct=0;
	                joypad[i-JOYPAD0].buttons ^= joypad[i-JOYPAD0].turbos;
	            }
	            break;
	          case MOUSE0: case MOUSE1:
	            c = mouse[i-MOUSE0].crosshair;
	            // NAC: Don't think we need this
	            /*
	            if(globals.IPPU.RenderThisFrame)
	                DrawCrosshair(GetCrosshair(c->img), c->fg, c->bg,
	                                 mouse[i-MOUSE0].cur_x, mouse[i-MOUSE0].cur_y);
	            */
	            break;
	          default:
	            break;
	        }
	    }

	    /*
	    for(n=0; n<8; n++){
	        if(!pseudopointer[n].mapped) continue;
	        if(pseudopointer[n].H_adj){
	            pseudopointer[n].x+=pseudopointer[n].H_adj;
	            if(pseudopointer[n].x<0) pseudopointer[n].x=0;
	            if(pseudopointer[n].x>255) pseudopointer[n].x=255;
	            if(pseudopointer[n].H_var){
	                if(pseudopointer[n].H_adj<0){
	                    if(pseudopointer[n].H_adj>-ptrspeeds[3]) pseudopointer[n].H_adj--;
	                } else {
	                    if(pseudopointer[n].H_adj<ptrspeeds[3]) pseudopointer[n].H_adj++;
	                }
	            }
	        }
	        if(pseudopointer[n].V_adj){
	            pseudopointer[n].y+=pseudopointer[n].V_adj;
	            if(pseudopointer[n].y<0) pseudopointer[n].y=0;
	            if(pseudopointer[n].y>PPU.ScreenHeight-1) pseudopointer[n].y=PPU.ScreenHeight-1;
	            if(pseudopointer[n].V_var){
	                if(pseudopointer[n].V_adj<0){
	                    if(pseudopointer[n].V_adj>-ptrspeeds[3]) pseudopointer[n].V_adj--;
	                } else {
	                    if(pseudopointer[n].V_adj<ptrspeeds[3]) pseudopointer[n].V_adj++;
	                }
	            }
	        }
	        ReportPointer(PseudoPointerBase+n, pseudopointer[n].x, pseudopointer[n].y);
	    }

	    set<struct exemulti *>::iterator it, jt;
	    for(it=exemultis.begin(); it!=exemultis.end(); it++){
	        i=ApplyMulti((*it)->script, (*it)->pos, (*it)->data1);
	        if(i>=0){
	            (*it)->pos=i;
	        } else {
	            jt=it;
	            it--;
	            delete *jt;
	            exemultis.erase(jt);
	        }
	    }
	    */
	    
	    do_polling(NUMCTLS);
	    //MovieUpdate();
		pad_read_last = pad_read;
		pad_read = false;
	}
	
	private void do_polling(int mp)
	{
		/*
	    set<uint32>::iterator itr;

	    if(pollmap[mp].empty()) return;

	    for(itr=pollmap[mp].begin(); itr!=pollmap[mp].end(); itr++){
	        switch(maptype(keymap[*itr].type)){
	          case MAP_BUTTON:
	            bool pressed;
	            if(PollButton(*itr, &pressed)) ReportButton(*itr, pressed);
	            break;

	          case MAP_AXIS:
	            int16 value;
	            if(PollAxis(*itr, &value)) ReportAxis(*itr, value);
	            break;

	          case MAP_POINTER:
	            int16 x, y;
	            if(PollPointer(*itr, &x, &y)) ReportPointer(*itr, x, y);
	            break;

	          default:
	            break;
	        }
	    }
	    */
	}
	
	void SetController(int port, int controller, int id1, int id2, int id3, int id4)
	{
		if( port < 0 || port > 1) return;
		
		switch( controller )
		{
		case CTL_NONE:
			break;
		case CTL_JOYPAD:
			if( id1 < 0 || id1 > 7)
				break;
			newcontrollers[port] = JOYPAD0 + id1;
			return;
		case CTL_MOUSE:
			if( id1 < 0 || id1 > 1)
				break;
			newcontrollers[port] = MOUSE0 + id1;
			return;
		}
		
		newcontrollers[port]=NONE;
	}
	
	private boolean FLAG_IOBIT0()
	{
		return (Memory.FillRAM.getByte(0x4213) & 0x40) != 0;
	}
	
	private boolean FLAG_IOBIT1()
	{
		return (Memory.FillRAM.getByte(0x4213) & 0x80) != 0;
	}
	
	private boolean FLAG_IOBIT(int n)
	{
		return n != 0 ? FLAG_IOBIT1() : FLAG_IOBIT0();
	}
}
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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public final class GLDisplay 
{
	private GLGeneric gl;
	
	private int DisplayHeight = 0;
	
	private static final int NumberLayers = 5;
	
	private static final short GL_TRANSPARENT_COLOR = 1;
	
	private short TileLookup[][] = new short[8][4096];
	
	// 8x8 tile buffer
	private static ShortBuffer TileBuffer = newShortBuffer( 64 );
	
	// Dimensions of the texture tile cache buffer
	private static final int TextureDimension = 1024;
	
	// Texture mapping dimensions of an 8x8 tile 
	private static final float TextureCoordTileDimensionFloat = (float) ((float) 8 / (float) TextureDimension);
	
	private static final int TextureCoordTileDimensionInt = 64;

	private int[] TextureHandle = new int[1];
	
	private int[] VertexHandle = new int[NumberLayers];
	
	private int[] TexCoordHandle = new int[NumberLayers];
	
	private FloatBuffer texcoordTempBufferFloat = newFloatBuffer( 6 * 2 );
	
	private IntBuffer texcoordTempBufferInt = newIntBuffer( 6 * 2 );
	
	private ShortBuffer vertexTempBuffer = newShortBuffer( 6 * 3 );

	// Next free column in the texture
	private int NextColumnPosition = 0;
	
	// Keeps next open position in the texture for the four tile sizes
	private int[] NextTileSizePosition = new int[4];
	
	private static final int Tile8x8 = 0;
	private static final int Tile16x16 = 1;
	private static final int Tile32x32 = 2;
	private static final int Tile64x64 = 3;
	
	private static final int[] SpriteSizeMap = new int[]
	{
		Tile8x8, Tile8x8, Tile8x8, Tile16x16, Tile16x16, Tile32x32, Tile32x32, Tile32x32, // Mode 0 
		Tile16x16, Tile32x32, Tile64x64, Tile32x32, Tile64x64, Tile64x64, Tile64x64, Tile32x32 // Mode 1
	};

	// TileMapModeSize	Map mode size (32x32, 64x32, 32x64 or 64x64)
	private static final int[] TileMapModeSize = new int[] { 2048, 4096, 4096, 8192 };

	private static final int BGTexureMapSize = (65 * 65) * 2 * 6;
	private static final int BGVertexMapSize = (65 * 65) * 3 * 6;

	private static final int SpriteVBOIndex = 4;
	private static final int SpriteTexureMapSize = 128 * 2 * 6;
	private static final int SpriteVertexMapSize = 128 * 3 * 6;

	private static final boolean UseJogl = true;

	private Globals globals;
	private PPU ppu;
	private Memory memory;
	
	private int framecount = 0;
	
	private boolean update = false;
	
	private int BgRed;
	private int BgBlue;
	private int BgGreen;
	
	void setUp()
	{
		globals = Globals.globals;
		ppu = globals.ppu;
		memory = globals.memory;
	}
	
	// Initialize graphics context
    public void init(GLGeneric glimpl)
    {
    	gl = glimpl;

		gl.glShadeModel(GLGeneric.GL_FLAT);

		// Depth test
		gl.glEnable(GLGeneric.GL_DEPTH_TEST);
		gl.glClearDepthf(1.0f);
		gl.glDepthFunc(GLGeneric.GL_LESS);
		
		// Scissor
		gl.glEnable(GLGeneric.GL_SCISSOR_TEST);
		
		// Alpha 
		gl.glEnable(GLGeneric.GL_ALPHA_TEST);
		gl.glAlphaFunc(GLGeneric.GL_NOTEQUAL, 1.0f);  // Don't render if alpha value is one
		
		// Textures
		gl.glEnable(GLGeneric.GL_TEXTURE_2D);
		
		gl.glPixelStorei(GLGeneric.GL_UNPACK_ALIGNMENT, 1);
		
		gl.glGenTextures(1, TextureHandle, 0);
		
		gl.glBindTexture(GLGeneric.GL_TEXTURE_2D, TextureHandle[0]);
		
		gl.glTexImage2D(GLGeneric.GL_TEXTURE_2D, 0, GLGeneric.GL_RGBA, TextureDimension, TextureDimension, 0, GLGeneric.GL_RGBA, GLGeneric.GL_UNSIGNED_SHORT_5_5_5_1, null);
		
		gl.glTexParameteri(GLGeneric.GL_TEXTURE_2D, GLGeneric.GL_TEXTURE_WRAP_S, GLGeneric.GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GLGeneric.GL_TEXTURE_2D, GLGeneric.GL_TEXTURE_WRAP_T, GLGeneric.GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GLGeneric.GL_TEXTURE_2D, GLGeneric.GL_TEXTURE_MAG_FILTER, GLGeneric.GL_NEAREST);
		gl.glTexParameteri(GLGeneric.GL_TEXTURE_2D, GLGeneric.GL_TEXTURE_MIN_FILTER, GLGeneric.GL_NEAREST);

		// Setup vertices
		gl.glGenBuffers(NumberLayers, VertexHandle, 0);
		
		for( int i = 0; i < NumberLayers; i++ )
		{
			gl.glBindBuffer(GLGeneric.GL_ARRAY_BUFFER, VertexHandle[i]);
			
			// If this is the sprite layer
			if ( i == SpriteVBOIndex )
			{
				gl.glBufferData(GLGeneric.GL_ARRAY_BUFFER, SpriteVertexMapSize * 2, null, GLGeneric.GL_DYNAMIC_DRAW );
			}
			else
			{
				gl.glBufferData(GLGeneric.GL_ARRAY_BUFFER, BGVertexMapSize * 2, null, GLGeneric.GL_DYNAMIC_DRAW );
			}
			
			gl.glBindBuffer(GLGeneric.GL_ARRAY_BUFFER, 0);
		}
		
		// Setup texture coordinate memory buffers
    	gl.glGenBuffers(NumberLayers, TexCoordHandle, 0);
		
		for( int i = 0; i < NumberLayers; i++ )
		{
			gl.glBindBuffer(GLGeneric.GL_ARRAY_BUFFER, TexCoordHandle[i]);

			// If this is the sprite layer
			if ( i == SpriteVBOIndex )
			{
				gl.glBufferData(GLGeneric.GL_ARRAY_BUFFER, SpriteTexureMapSize * 4, null, GLGeneric.GL_DYNAMIC_DRAW );
			}
			else
			{
				gl.glBufferData(GLGeneric.GL_ARRAY_BUFFER, BGTexureMapSize * 4, null, GLGeneric.GL_DYNAMIC_DRAW );
			}
			
			gl.glBindBuffer(GLGeneric.GL_ARRAY_BUFFER, 0);
		}

		TileBuffer.rewind();
    }
    
    public void reshape(int x, int y, int w, int h)
	{
		gl.glViewport(0, 0, w, h);
		//gl.glMatrixMode(GLGeneric.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glOrthox(0, w, h, 0, -128, 128);		
		gl.glMatrixMode(GLGeneric.GL_MODELVIEW);
		
		DisplayHeight = h;
	}

    public void display()
    {
    	if (ppu.ForcedBlanking)
    	{
    		gl.glClearColorx( 0, 0, 0, 0);
    		gl.glClear(GLGeneric.GL_COLOR_BUFFER_BIT);
    		return;
    	}

    	// Basic frameskipping
    	framecount++;
    	
    	if (framecount > 6)
    	{
    		framecount = 0;
    	}
    	else
    	{
    		//return;
    	}
    	
    	/*
    	try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
    	
		RenderScreen();
		
		showTextures();
		//showPalette();
		//dumpTileMap();
    }
    
    private void showTextures()
    {
    
    	javax.media.opengl.GL mygl = (javax.media.opengl.GL) this.gl;
    	
    	mygl.glBegin(javax.media.opengl.GL.GL_QUADS);
    	mygl.glTexCoord2f(0f, 0f);
    	mygl.glVertex3f(0f, 512f, 0f);
		
    	mygl.glTexCoord2f(0f, 1f);
    	mygl.glVertex3f(0f, 1536f, 0f);
		
    	mygl.glTexCoord2f(1, 1);
    	mygl.glVertex3f(1024f, 1536f, 0f);
		
    	mygl.glTexCoord2f(1f, 0f);
    	mygl.glVertex3f(1024f, 512f, 0f);
    	mygl.glEnd();
		
    }
    
    private void showPalette()
    {
    	/*
    	gl.glDisable(GLGeneric.GL_TEXTURE_2D);
    	gl.glDisable(GLGeneric.GL_DEPTH_TEST);
    	gl.glDisable(GLGeneric.GL_ALPHA_TEST);

    	int left = 256;
    	int size = 16;
    	
    	for(int i = 0; i < ppu.CGDATA.length; i++)
	    {
        	int bgr = ppu.CGDATA[i];
        	
        	gl.glColor4f( ( bgr & 0x1F ) / 31.0f, ( ( bgr & 0x03E0 ) >> 5 ) / 31.0f, ( ( bgr & 0x7C00 ) >> 10 ) / 31.0f, 0f);

    		int x = ((i & 0x0F) * size) + left;
    		int y = ((i & 0xF0) >> 4 ) * size;
    		
			gl.glBegin(GLGeneric.GL_QUADS);
				
			gl.glVertex2f((float)x, (float)y );
			gl.glVertex2f((float)x, (float)(y + size) );
			gl.glVertex2f((float)(x + size), (float)(y + size) );
			gl.glVertex2f((float)(x + size), (float)y );
			
			gl.glEnd();
	    }
    	
    	gl.glEnable(GLGeneric.GL_TEXTURE_2D);
    	gl.glEnable(GLGeneric.GL_DEPTH_TEST);
    	gl.glEnable(GLGeneric.GL_ALPHA_TEST);
    	*/
    }
    
    private void dumpTileMap()
    {
    	if (update)
		{
			int a = ppu.BG[0].TileMapAddress;
			
	    	int TileMapLength = TileMapModeSize[0];
	    	
			// Loop through tile map in 64 byte chunks
			for( int i = 0; i < TileMapLength; i = i + 2 )
			{
				if ( ( i & 0x3F) == 0 )
					System.out.println();
				
				int tile = memory.VRAM.get16Bit(a + i);
				
				System.out.format("%04X,", tile);
			}
			
			System.exit(0);
		}
    }
    
    void setBackgroundColor(int Red, int Green, int Blue )
    {
    	this.BgRed = Red;
    	this.BgGreen = Green;
    	this.BgBlue = Blue;
    }

    /**
     * Main SNES drawing routine
     */
    private final void RenderScreen()
    {
    	gl.glScissor(0, DisplayHeight - (ppu.ScreenHeight), 255, ppu.ScreenHeight );
    	
    	byte brightness = (byte) ppu.Brightness;
    	
    	gl.glColor4ub(brightness, brightness, brightness, (byte)255 );
    	
    	gl.glClearColorx( BgRed, BgGreen, BgBlue, 0);
    	
		gl.glClear(GLGeneric.GL_COLOR_BUFFER_BIT | GLGeneric.GL_DEPTH_BUFFER_BIT);

		gl.glEnableClientState(GLGeneric.GL_VERTEX_ARRAY);
		gl.glEnableClientState(GLGeneric.GL_TEXTURE_COORD_ARRAY);
		
		// Draw the PPU background mode
    	switch(ppu.BGMode)
		{
		case 0:
			DrawBackground(0,  0, PPU.TILE_2BIT, false, false, 15, 11, 0);
			DrawBackground(1, 32, PPU.TILE_2BIT, false, false, 14, 10, 0);
			DrawBackground(2, 64, PPU.TILE_2BIT, false, false,  7,  3, 0);
			DrawBackground(3, 96, PPU.TILE_2BIT, false, false,  6,  2, 0);
			break;
		case 1:
			DrawBackground(0, 0, PPU.TILE_4BIT, false, false, 15, 11, 0);
			DrawBackground(1, 0, PPU.TILE_4BIT, false, false, 14, 10, 0);
			DrawBackground(2, 0, PPU.TILE_2BIT, false, false, (ppu.BG3Priority ? 17 : 7 ),  3, 0);			
			break;
		case 2:
			DrawBackground(0, 0, PPU.TILE_4BIT, false, true, 15, 7, 8);
			DrawBackground(1, 0, PPU.TILE_4BIT, false, true, 11, 3, 8);
			break;
		case 3:
			DrawBackground(0, 0, PPU.TILE_8BIT, false, false, 15, 7, 0);
			DrawBackground(1, 0, PPU.TILE_4BIT, false, false, 11, 3, 0);
			break;
		case 4:
			DrawBackground(0, 0, PPU.TILE_8BIT, false, true, 15, 7, 0);
			DrawBackground(1, 0, PPU.TILE_2BIT, false, true, 11, 3, 0);
			break;
		case 5:
			DrawBackground(0, 0, PPU.TILE_4BIT, true, false, 15, 7, 0);
			DrawBackground(1, 0, PPU.TILE_2BIT, true, false, 11, 3, 0);
			break;
		case 6:
			DrawBackground(0, 0, PPU.TILE_4BIT, true, true, 15, 7, 8);
			break;
		case 7:
			DrawMode7();
			break;
		}
    	
    	if ( ( memory.FillRAM.getByte( 0x212c ) & 0x10 ) != 0 )
    	{
    		DrawSprites();
    	}
    	
    	gl.glDisableClientState(GLGeneric.GL_TEXTURE_COORD_ARRAY);
    	gl.glDisableClientState(GLGeneric.GL_VERTEX_ARRAY);
		
		if (ppu.ColorsChanged)
			ppu.ColorsChanged = false;
    }
   
	/**
     * Draws a background layer
     * 
     * @param BGNumber			Number of the background
     * @param palette_base		Palette offset in which all palette start from
     * @param depth				Color bit depth
     * @param hires				Interlaced mode
     * @param offset			???
     * @param zHeightPriority	Depth of tiles with the priority bit set
     * @param zHeightNormal		Default background depth
     * @param voffoff			???
     */
    private void DrawBackground(int BGNumber, int palette_base, int depth, boolean hires, boolean offset, int zHeightPriority, int zHeightNormal, int voffoff )
    {
    	PPU.Bg Bg = ppu.BG[BGNumber];

    	int TileMapLength = TileMapModeSize[Bg.TileMapSize];
    	
    	int tile_lookup;
    	
    	int TileDimension = Bg.TileSizeMode == 0 ? 8 : 16;
    		
		// Loop through tile map in 64 byte chunks
		for( int i = 0; i < TileMapLength; i = i + 64 )
		{
			// Lookup address of 64 byte tile block
			int tile_map_block = (Bg.TileMapAddress + i) >> 6;

			// If tile map block is dirty
			boolean updateTileMap = ppu.TileCached[PPU.TILE_8BIT][tile_map_block] == 0;
			
			if ( updateTileMap )
			{
				// Mark tile map entry as clean now
				ppu.TileCached[PPU.TILE_8BIT][tile_map_block] = 1;
			}
    
			// Process 64 byte chunk
			for( int h = i; h < i + 64; h = h + 2)
			{
				// Get raw tile map entry
				int tile_map_entry = memory.VRAM.get16Bit( Bg.TileMapAddress + h);
				
				int palette = ( tile_map_entry >> 10 ) & 0x7; 
				
				int tile_pixel_data_address = Bg.TileDataBase + ( (tile_map_entry & 0x3FF) * ((depth + 1) << 4) );
				
				int tile_lookup_address = tile_pixel_data_address >> (4 + depth);
				
				tile_lookup = TileLookup[palette][tile_lookup_address];
				
				//int palette_lookup = palette_base + ((palette << depth) << 3);

				// If tile pixels needed decoding
				if ( tile_lookup == 0 )
				{
					switch(depth)
					{
					case PPU.TILE_2BIT:
						DecodeTile2(tile_pixel_data_address, palette_base + (palette << 2) );
						break;
					case PPU.TILE_4BIT:
						DecodeTile4(tile_pixel_data_address, palette << 4 );
						break;
					case PPU.TILE_8BIT:
						DecodeTile8(tile_pixel_data_address);
						break;
					}
					
					// Get the next available texture index position
					tile_lookup = getNextTilePosition(Bg.TileSizeMode);					
					
					// Calculate the x and y texture coordinates from TilePosition
					int texture_x = tile_lookup & 1016; 
					int texture_y = ((tile_lookup & 7) << 3 ) + ((15360 & tile_lookup) >> 4);
					
					// Copy decoded tile to GL memory
					gl.glBindTexture(GLGeneric.GL_TEXTURE_2D, TextureHandle[0]);
					gl.glTexSubImage2D(GLGeneric.GL_TEXTURE_2D, 0, texture_x, texture_y, 8, 8, GLGeneric.GL_RGBA, GLGeneric.GL_UNSIGNED_SHORT_5_5_5_1, TileBuffer );

					// Update tile lookup map 
					TileLookup[palette][tile_lookup_address] = (short) tile_lookup;

					// If we reach the end of the texture map then reset
					if ( tile_lookup >= 16384)
					{
						ResetTileLookups();
						//System.out.println("SGNES: Out of tile space");
						//System.exit(0);
					}
				}
				// If tile is dirty then update existing tile
				else if ( ppu.TileCached[depth][tile_lookup_address] == 0 )
				{
					//ppu.PaletteCached[palette_lookup >> 4] = 1;
					
					switch(depth)
					{
					case PPU.TILE_2BIT:
						DecodeTile2(tile_pixel_data_address, palette_base + (tile_map_entry >> 8) & 0x1C);
						break;
					case PPU.TILE_4BIT:
						DecodeTile4(tile_pixel_data_address, (tile_map_entry >> 6) & 0x70 );
						break;
					case PPU.TILE_8BIT:
						DecodeTile8(tile_pixel_data_address);
						break;
					}

					// Calculate the x and y texture coordinates from tile_lookup
					int texture_x = (tile_lookup & 1016); 
					int texture_y = ((tile_lookup & 7) << 3 ) + ((15360 & tile_lookup) >> 4);

					// Copy decoded tile into GL memory					
					gl.glBindTexture(GLGeneric.GL_TEXTURE_2D, TextureHandle[0]);
					gl.glTexSubImage2D(GLGeneric.GL_TEXTURE_2D, 0, texture_x, texture_y, 8, 8, GLGeneric.GL_RGBA, GLGeneric.GL_UNSIGNED_SHORT_5_5_5_1, TileBuffer );
										
					// Mark tile map block as clean
					ppu.TileCached[depth][tile_lookup_address] = 1;
				}
				
				// If the tile map properties where changed then update
				if( updateTileMap )
				{					
					// Computationally determine the x and y locations of the tile map
					// based upon the map size and map entry position
					// Note: Each map is composed of 32x32 block sequences
					// |32x32|32x32|
					// |32x32|32x32|
					
					int tile_position_base = 0x7FF & h;

					int tile_x = ( ( tile_position_base ) & 63 ) >> 1; 
					int tile_y = ( tile_position_base ) >> 6;

					/*
					switch(Bg.TileMapSize)
					{
					// 32x32
					case 0:
						break;
					// 64x32
					case 1:
						tile_x += ( ( (h >> 11) & 1 ) << 5 );
						break;
					// 32x64
					case 2:
						tile_y += ( ( (h >> 10) & 2 ) << 4 ); 
						break;
					// 64x64
					case 3:
						switch ( h >> 11 )
						{
						case 1:
							tile_x += 32;
							break;
						case 2:
							tile_y += 32;
							break;
						case 3:
							tile_x += 32;
							tile_y += 32;
							break;
						}
						break;
					}
					*/
					tile_x = tile_x * TileDimension;
					tile_y = tile_y * TileDimension;
					
					// Get set to ( 0 or 1 ) if priority bit is set
					short zIndex = (short)(((0x2000 & tile_map_entry) != 0) ? (zHeightPriority - zHeightNormal) : 0);
									
					// Set tile vertices
					
					// Make a quad with two triangles 
					// Triangle 1 - |\
					vertexTempBuffer.rewind();
					
					// Top left
					vertexTempBuffer.put( (short) tile_x );
					vertexTempBuffer.put( (short) tile_y );
					vertexTempBuffer.put( zIndex );

					// Bottom left
					vertexTempBuffer.put( (short) tile_x );
					vertexTempBuffer.put( (short) (tile_y + TileDimension) );
					vertexTempBuffer.put( zIndex );
					
					// Bottom right
					vertexTempBuffer.put( (short) ( tile_x + TileDimension ) );
					vertexTempBuffer.put( (short) ( tile_y + TileDimension ) );
					vertexTempBuffer.put( zIndex );
					
					// Triangle 2 - \|

					// Top left
					vertexTempBuffer.put( (short) tile_x );
					vertexTempBuffer.put( (short) tile_y );
					vertexTempBuffer.put( zIndex );
			
					// Top right
					vertexTempBuffer.put( (short) ( tile_x + TileDimension ) );
					vertexTempBuffer.put( (short) tile_y );
					vertexTempBuffer.put( zIndex );

					// Bottom right
					vertexTempBuffer.put( (short) ( tile_x + TileDimension ) );
					vertexTempBuffer.put( (short) ( tile_y + TileDimension ) );
					vertexTempBuffer.put( zIndex );
									
					vertexTempBuffer.rewind();
					
					gl.glBindBuffer(GLGeneric.GL_ARRAY_BUFFER, VertexHandle[BGNumber]);
					gl.glBufferSubData(GLGeneric.GL_ARRAY_BUFFER, h * 9 * 2, 18 * 2, vertexTempBuffer);
					
					if ( UseJogl )
					{
						// Set tile texture coordinates
						float texture_coord_x = ( (float) ((tile_lookup & 1016) >> 3 ) ) * TextureCoordTileDimensionFloat;  
						float texture_coord_y = ( (float) ( ( tile_lookup & 7 ) + ( (15360 & tile_lookup) >> 7) ) ) * TextureCoordTileDimensionFloat;
						
						texcoordTempBufferFloat.rewind();
						
						float texture_coord_tile_dimension = Bg.TileSizeMode == 0 ? TextureCoordTileDimensionFloat : TextureCoordTileDimensionFloat * 2f;
	
						// Based on horizontal / vertical bits goto appropriate texture coordinate mapping  
						switch( tile_map_entry >> 14 )
						{
						// Normal
						case 0:
							// Triangle 1 - |\
							
							// Top left
							texcoordTempBufferFloat.put( texture_coord_x );
							texcoordTempBufferFloat.put( texture_coord_y );
	
							// Bottom left
							texcoordTempBufferFloat.put( texture_coord_x );
							texcoordTempBufferFloat.put( texture_coord_y + texture_coord_tile_dimension );
							
							// Bottom right
							texcoordTempBufferFloat.put( texture_coord_x + texture_coord_tile_dimension );
							texcoordTempBufferFloat.put( texture_coord_y + texture_coord_tile_dimension );
	
							// Triangle 2 - \|
	
							// Top left
							texcoordTempBufferFloat.put( texture_coord_x );
							texcoordTempBufferFloat.put( texture_coord_y );
													
							// Top right
							texcoordTempBufferFloat.put( texture_coord_x + texture_coord_tile_dimension);
							texcoordTempBufferFloat.put( texture_coord_y );
							
							// Bottom right
							texcoordTempBufferFloat.put( texture_coord_x + texture_coord_tile_dimension);
							texcoordTempBufferFloat.put( texture_coord_y + texture_coord_tile_dimension);
	
							break;
							
						// Horizontal flip
						case 2:
							// Triangle 1 - |\
							
							// Top left
							texcoordTempBufferFloat.put( texture_coord_x );
							texcoordTempBufferFloat.put( texture_coord_y + texture_coord_tile_dimension);
	
							// Bottom left
							texcoordTempBufferFloat.put( texture_coord_x );
							texcoordTempBufferFloat.put( texture_coord_y );
							
							// Bottom right
							texcoordTempBufferFloat.put( texture_coord_x + texture_coord_tile_dimension);
							texcoordTempBufferFloat.put( texture_coord_y );
	
							// Triangle 2 - \|
	
							// Top left
							texcoordTempBufferFloat.put( texture_coord_x );
							texcoordTempBufferFloat.put( texture_coord_y + texture_coord_tile_dimension );
							
							// Bottom right
							texcoordTempBufferFloat.put( texture_coord_x + texture_coord_tile_dimension);
							texcoordTempBufferFloat.put( texture_coord_y + texture_coord_tile_dimension);
							
							// Top right
							texcoordTempBufferFloat.put( texture_coord_x + texture_coord_tile_dimension);
							texcoordTempBufferFloat.put( texture_coord_y );
							break;
							
						// Vertical flip
						case 1:
							// Triangle 1 - |\
							
							// Top left
							texcoordTempBufferFloat.put( texture_coord_x + texture_coord_tile_dimension);
							texcoordTempBufferFloat.put( texture_coord_y );
	
							// Bottom left
							texcoordTempBufferFloat.put( texture_coord_x + texture_coord_tile_dimension);
							texcoordTempBufferFloat.put( texture_coord_y + texture_coord_tile_dimension);
							
							// Bottom right
							texcoordTempBufferFloat.put( texture_coord_x );
							texcoordTempBufferFloat.put( texture_coord_y + texture_coord_tile_dimension);
	
							// Triangle 2 - \|
	
							// Top left
							texcoordTempBufferFloat.put( texture_coord_x + texture_coord_tile_dimension);
							texcoordTempBufferFloat.put( texture_coord_y );
							
							// Bottom right
							texcoordTempBufferFloat.put( texture_coord_x );
							texcoordTempBufferFloat.put( texture_coord_y );
							
							// Top right
							texcoordTempBufferFloat.put( texture_coord_x );
							texcoordTempBufferFloat.put( texture_coord_y + texture_coord_tile_dimension);
							break;
							
						// Horizontal and Vertical flip
						case 3:
							// Triangle 1 - |\
							
							// Top left
							texcoordTempBufferFloat.put( texture_coord_x + texture_coord_tile_dimension);
							texcoordTempBufferFloat.put( texture_coord_y + texture_coord_tile_dimension);
	
							// Bottom left
							texcoordTempBufferFloat.put( texture_coord_x  + texture_coord_tile_dimension);
							texcoordTempBufferFloat.put( texture_coord_y );
							
							// Bottom right
							texcoordTempBufferFloat.put( texture_coord_x );
							texcoordTempBufferFloat.put( texture_coord_y );
	
							// Triangle 2 - \|
	
							// Top left
							texcoordTempBufferFloat.put( texture_coord_x + texture_coord_tile_dimension);
							texcoordTempBufferFloat.put( texture_coord_y + texture_coord_tile_dimension);
							
							// Bottom right
							texcoordTempBufferFloat.put( texture_coord_x );
							texcoordTempBufferFloat.put( texture_coord_y + texture_coord_tile_dimension);
							
							// Top right
							texcoordTempBufferFloat.put( texture_coord_x );
							texcoordTempBufferFloat.put( texture_coord_y );
							break;
						}
						
						texcoordTempBufferFloat.rewind();
	
						// Copy updated data to GL texture buffer
						gl.glBindBuffer(GLGeneric.GL_ARRAY_BUFFER, TexCoordHandle[BGNumber]);
						gl.glBufferSubData(GLGeneric.GL_ARRAY_BUFFER, h * 6 * 4, 12 * 4, texcoordTempBufferFloat);
					}
					// OpenGL ES
					else
					{
						// Set tile texture coordinates
						int texture_coord_x = ( (tile_lookup & 1016) >> 3 ) * TextureCoordTileDimensionInt;  
						int texture_coord_y = ( ( tile_lookup & 7 ) + ( (15360 & tile_lookup) >> 7) ) * TextureCoordTileDimensionInt;
						
						texcoordTempBufferInt.rewind();
						
						int texture_coord_tile_dimension = Bg.TileSizeMode == 0 ? TextureCoordTileDimensionInt : TextureCoordTileDimensionInt * 2;
	
						// Based on horizontal / vertical bits goto appropriate texture coordinate mapping  
						switch( tile_map_entry >> 14 )
						{
						// Normal
						case 0:
							// Triangle 1 - |\
							
							// Top left
							texcoordTempBufferInt.put( texture_coord_x );
							texcoordTempBufferInt.put( texture_coord_y );
	
							// Bottom left
							texcoordTempBufferInt.put( texture_coord_x );
							texcoordTempBufferInt.put( texture_coord_y + texture_coord_tile_dimension );
							
							// Bottom right
							texcoordTempBufferInt.put( texture_coord_x + texture_coord_tile_dimension );
							texcoordTempBufferInt.put( texture_coord_y + texture_coord_tile_dimension );
	
							// Triangle 2 - \|
	
							// Top left
							texcoordTempBufferInt.put( texture_coord_x );
							texcoordTempBufferInt.put( texture_coord_y );
													
							// Top right
							texcoordTempBufferInt.put( texture_coord_x + texture_coord_tile_dimension);
							texcoordTempBufferInt.put( texture_coord_y );
							
							// Bottom right
							texcoordTempBufferInt.put( texture_coord_x + texture_coord_tile_dimension);
							texcoordTempBufferInt.put( texture_coord_y + texture_coord_tile_dimension);
	
							break;
							
						// Horizontal flip
						case 2:
							// Triangle 1 - |\
							
							// Top left
							texcoordTempBufferInt.put( texture_coord_x );
							texcoordTempBufferInt.put( texture_coord_y + texture_coord_tile_dimension);
	
							// Bottom left
							texcoordTempBufferInt.put( texture_coord_x );
							texcoordTempBufferInt.put( texture_coord_y );
							
							// Bottom right
							texcoordTempBufferInt.put( texture_coord_x + texture_coord_tile_dimension);
							texcoordTempBufferInt.put( texture_coord_y );
	
							// Triangle 2 - \|
	
							// Top left
							texcoordTempBufferInt.put( texture_coord_x );
							texcoordTempBufferInt.put( texture_coord_y + texture_coord_tile_dimension );
							
							// Bottom right
							texcoordTempBufferInt.put( texture_coord_x + texture_coord_tile_dimension);
							texcoordTempBufferInt.put( texture_coord_y + texture_coord_tile_dimension);
							
							// Top right
							texcoordTempBufferInt.put( texture_coord_x + texture_coord_tile_dimension);
							texcoordTempBufferInt.put( texture_coord_y );
							break;
							
						// Vertical flip
						case 1:
							// Triangle 1 - |\
							
							// Top left
							texcoordTempBufferInt.put( texture_coord_x + texture_coord_tile_dimension);
							texcoordTempBufferInt.put( texture_coord_y );
	
							// Bottom left
							texcoordTempBufferInt.put( texture_coord_x + texture_coord_tile_dimension);
							texcoordTempBufferInt.put( texture_coord_y + texture_coord_tile_dimension);
							
							// Bottom right
							texcoordTempBufferInt.put( texture_coord_x );
							texcoordTempBufferInt.put( texture_coord_y + texture_coord_tile_dimension);
	
							// Triangle 2 - \|
	
							// Top left
							texcoordTempBufferInt.put( texture_coord_x + texture_coord_tile_dimension);
							texcoordTempBufferInt.put( texture_coord_y );
							
							// Bottom right
							texcoordTempBufferInt.put( texture_coord_x );
							texcoordTempBufferInt.put( texture_coord_y );
							
							// Top right
							texcoordTempBufferInt.put( texture_coord_x );
							texcoordTempBufferInt.put( texture_coord_y + texture_coord_tile_dimension);
							break;
							
						// Horizontal and Vertical flip
						case 3:
							
							// Triangle 1 - |\
							
							// Top left
							texcoordTempBufferInt.put( texture_coord_x + texture_coord_tile_dimension);
							texcoordTempBufferInt.put( texture_coord_y + texture_coord_tile_dimension);
	
							// Bottom left
							texcoordTempBufferInt.put( texture_coord_x  + texture_coord_tile_dimension);
							texcoordTempBufferInt.put( texture_coord_y );
							
							// Bottom right
							texcoordTempBufferInt.put( texture_coord_x );
							texcoordTempBufferInt.put( texture_coord_y );
	
							// Triangle 2 - \|
	
							// Top left
							texcoordTempBufferInt.put( texture_coord_x + texture_coord_tile_dimension);
							texcoordTempBufferInt.put( texture_coord_y + texture_coord_tile_dimension);
							
							// Bottom right
							texcoordTempBufferInt.put( texture_coord_x );
							texcoordTempBufferInt.put( texture_coord_y + texture_coord_tile_dimension);
							
							// Top right
							texcoordTempBufferInt.put( texture_coord_x );
							texcoordTempBufferInt.put( texture_coord_y );
							break;
						}
						
						texcoordTempBufferInt.rewind();
	
						// Copy updated data to GL texture buffer
						gl.glBindBuffer(GLGeneric.GL_ARRAY_BUFFER, TexCoordHandle[BGNumber]);
						gl.glBufferSubData(GLGeneric.GL_ARRAY_BUFFER, h * 6 * 4, 12 * 4, texcoordTempBufferInt);
						
					}
				}			
			}
		}		

		// Texture coordinate points 
		gl.glBindBuffer(GLGeneric.GL_ARRAY_BUFFER, TexCoordHandle[BGNumber] );
		
		if ( UseJogl )
		{
			gl.glTexCoordPointer(2, GLGeneric.GL_FLOAT, 0, 0 );
		}
		else
		{
			gl.glTexCoordPointer(2, GLGeneric.GL_FIXED, 0, 0 );
		}

		
		// Setup vertices
		gl.glBindBuffer(GLGeneric.GL_ARRAY_BUFFER, VertexHandle[BGNumber]);
		gl.glVertexPointer(3, GLGeneric.GL_SHORT, 0, 0 );
		
		
		// Removed line based rendered
		/*	
		int StartLine = 0;
		int EndLine;

		short bgScanLines[] = ppu.BGScanLinePositions[BGNumber];
		// Process scan line blocks each 3 properties per array chunk
		for( int i = 0; i <= bgScanLines[0]; i = i + 3)
		{
			// If last row
			if (i == bgScanLines[0])
			{
				EndLine = ppu.ScreenHeight + PPU.FIRST_VISIBLE_LINE;
			}
			else
			{
				EndLine = ppu.BGScanLinePositions[BGNumber][i];
			}
			
			//int horizontal = bgScanLines[i + PPU.BGIND_HORIZONTAL] & ( ( ( ( (Bg.TileMapSize & 1) + 1) << (Bg.TileSizeMode + 8) ) - 1) ) ;
			//int vertical = bgScanLines[i + PPU.BGIND_VERTICAL] & ( ( ( ( (Bg.TileMapSize & 2) + 2) << (Bg.TileSizeMode + 7) ) - 1) );
		*/
		
		int horizontal = Bg.HOffset & ( ( ( ( (Bg.TileMapSize & 1) + 1) << (Bg.TileSizeMode + 8) ) - 1) ) ;
		int vertical = (Bg.VOffset + 1) & ( ( ( ( (Bg.TileMapSize & 2) + 2) << (Bg.TileSizeMode + 7) ) - 1) );

		//gl.glScissor(0, DisplayHeight - (EndLine - StartLine) - StartLine, 255, EndLine - StartLine);
		
		switch(Bg.TileMapSize)
		{
		// 32x32
		case 0:
			// Main tile map
			gl.glPushMatrix();
			gl.glTranslatex(
				-horizontal,
				-vertical,
				zHeightNormal
			);				
			gl.glDrawArrays(GLGeneric.GL_TRIANGLES, 0, 6144  );
			gl.glPopMatrix();		
			
			// Horizontal wrap
			gl.glPushMatrix();
			gl.glTranslatex(
				(horizontal - 255) * -1, 
				-vertical, 
				zHeightNormal
			);				
			gl.glDrawArrays(GLGeneric.GL_TRIANGLES, 0, 6144  );
			gl.glPopMatrix();

			// Vertical wrap
			gl.glPushMatrix();
			gl.glTranslatex(
				-horizontal,
				(vertical - 255) * -1,
				zHeightNormal
			);				
			gl.glDrawArrays(GLGeneric.GL_TRIANGLES, 0, 6144  );
			gl.glPopMatrix();

			// Horizontal and Vertical wrap
			gl.glPushMatrix();
			gl.glTranslatex(
				(horizontal - 255) * -1,
				(vertical - 255) * -1,
				zHeightNormal
			);				
			gl.glDrawArrays(GLGeneric.GL_TRIANGLES, 0, 6144  );
			gl.glPopMatrix();
			break;
			
		// 64x32
		case 1:
			// Left tile map
			gl.glPushMatrix();
			gl.glTranslatex(
					( horizontal & 0x100 ) - ( horizontal & 0xFF ),
					-vertical,
					zHeightNormal );				
			gl.glDrawArrays(GLGeneric.GL_TRIANGLES, 0, 6144 );
			gl.glPopMatrix();
			
			// Left map vertical wrap
			gl.glPushMatrix();
			gl.glTranslatex(
					( horizontal & 0x100 ) - ( horizontal & 0xFF ),
					(vertical - 255) * -1,
					zHeightNormal );				
			gl.glDrawArrays(GLGeneric.GL_TRIANGLES, 0, 6144 );
			gl.glPopMatrix();
			

			// Right tile map
			gl.glPushMatrix();
			gl.glTranslatex(
					-horizontal,
					-vertical,
					zHeightNormal );				
			gl.glDrawArrays(GLGeneric.GL_TRIANGLES, 6144, 6144 );
			gl.glPopMatrix();

			// Right map vertical wrap
			gl.glPushMatrix();
			gl.glTranslatex(
					-horizontal,
					(vertical - 255) * -1,
					zHeightNormal );				
			gl.glDrawArrays(GLGeneric.GL_TRIANGLES, 6144, 6144 );
			gl.glPopMatrix();

			
			break;
		// 32x64
		case 2:

			// Top tile map
			gl.glPushMatrix();
			
			gl.glTranslatex(
					-horizontal,
					( vertical & 0x100 ) - ( vertical & 0xFF ),
					zHeightNormal );
							
			gl.glDrawArrays(GLGeneric.GL_TRIANGLES, 0, 6144 );
			gl.glPopMatrix();
			
			// Top map horizontal wrap
			gl.glPushMatrix();
			gl.glTranslatex(
					(horizontal - 255) * -1,
					( vertical & 0x100 ) - ( vertical & 0xFF ),
					zHeightNormal );				
			gl.glDrawArrays(GLGeneric.GL_TRIANGLES, 0, 6144 );
			gl.glPopMatrix();
			

			// Bottom tile map
			gl.glPushMatrix();
			gl.glTranslatex(
					( horizontal & 0x100  ) - ( horizontal & 0xFF ),
					-vertical,
					zHeightNormal );				
			gl.glDrawArrays(GLGeneric.GL_TRIANGLES, 6144, 6144 );
			gl.glPopMatrix();
			
			// Bottom map horizontal wrap
			gl.glTranslatex(
					( horizontal & 0x100  ) - ( horizontal & 0xFF ),
					(vertical - 256) * -1,
					zHeightNormal );				
			gl.glDrawArrays(GLGeneric.GL_TRIANGLES, 6144, 6144 );
			gl.glPopMatrix();


			break;
		// 64x64
		case 3:
			// Top left
			gl.glPushMatrix();
			gl.glTranslatex(
					( horizontal & 0x100 ) - ( horizontal & 0xFF ),
					( vertical & 0x100 ) - ( vertical & 0xFF ),
					zHeightNormal );				
			gl.glDrawArrays(GLGeneric.GL_TRIANGLES, 0, 6144 );
			gl.glPopMatrix();

			
			// Top right
			gl.glPushMatrix();
			gl.glTranslatex(
					(-horizontal) + 0x100,
					( vertical & 0x100 ) - ( vertical & 0xFF ),
					zHeightNormal );				
			gl.glDrawArrays(GLGeneric.GL_TRIANGLES, 6144, 6144 );
			gl.glPopMatrix();
			
			// Bottom left
			gl.glPushMatrix();
			gl.glTranslatex(
					( horizontal & 0x100  ) - ( horizontal & 0xFF ),
					(-vertical) + 0x100,
					zHeightNormal );				
			gl.glDrawArrays(GLGeneric.GL_TRIANGLES, 12288, 6144 );
			gl.glPopMatrix();
			
			// Bottom right
			gl.glPushMatrix();
			gl.glTranslatex(
					(-horizontal) + 0x100,
					(-vertical) + 0x100,
					zHeightNormal );				
			gl.glDrawArrays(GLGeneric.GL_TRIANGLES, 18432, 6144 );
			gl.glPopMatrix();
			
			break;
		}
		
		/*
			StartLine = EndLine + 1;
		}
		*/
    }
    
    /**
     * Draw mode7 background
     */
    private void DrawMode7()
    {
		
	}
	   
    /**
     * Decode a 2 bit tile address to TileBuffer
     * 
     * @param tile_address
     * @param tile
     * @param palette_offset
     */
    private void DecodeTile2( int tile_address, int palette_offset )
    {		
    	// Decode 8 lines each 8 pixels wide
		for( int end = tile_address + 16; tile_address < end; tile_address = tile_address + 2 )
		{			
			int byte1 = memory.VRAM.buffer[tile_address];
			int byte2 = memory.VRAM.buffer[tile_address + 1];
			
			int mask = 128;
			
			while ( mask > 0 )
			{
				int pixel = ( ( byte1 & mask ) != 0 ? 1 : 0 ) |
							( ( byte2 & mask ) != 0 ? 2 : 0 );
				
				if ( pixel == 0 )
				{
					// Set transparent
					TileBuffer.put( GL_TRANSPARENT_COLOR );
				}
				else
				{
					TileBuffer.put( ppu.CGDATA[palette_offset + pixel] );
				}
				
				mask = mask >> 1;
			}
		}

		TileBuffer.rewind();
    }

    /**
     * Decode a 4 bit tile address to TileBuffer
     * 
     * @param tile_address
     * @param palette_base
     */
    private void DecodeTile4( int tile_address, int palette_base )
    {
    	// Decode 8 lines each 8 pixels wide
    	for( int end = tile_address + 16; tile_address < end; tile_address = tile_address + 2 )
		{			
			int byte1 = memory.VRAM.buffer[tile_address];
			int byte2 = memory.VRAM.buffer[tile_address + 1];
			int byte3 = memory.VRAM.buffer[tile_address + 16];
			int byte4 = memory.VRAM.buffer[tile_address + 17];
			
			int mask = 128;

			while ( mask > 0 )
			{
				int pixel = ( ( byte1 & mask ) != 0 ? 1 : 0 ) |
						( ( byte2 & mask ) != 0 ? 2 : 0 ) |
						( ( byte3 & mask ) != 0 ? 4 : 0 ) |
						( ( byte4 & mask ) != 0 ? 8 : 0 ) ;

				if ( pixel == 0 )
				{
					// Set transparent
					TileBuffer.put( GL_TRANSPARENT_COLOR );
				}
				else
				{					
					TileBuffer.put( ppu.CGDATA[palette_base + pixel] );
				}

				mask = mask >> 1;
			}
		}
    	
    	TileBuffer.rewind();
    }
    
    /**
     * Decode a 8bit tile address to TileBuffer
     * 
     * @param tile_address
     */
    private void DecodeTile8(int tile_address)
    {    	
    	// Decode 8 lines each 8 pixels wide
    	for( int end = tile_address + 16; tile_address < end; tile_address = tile_address + 2 )
		{			
			int byte1 = memory.VRAM.buffer[tile_address];
			int byte2 = memory.VRAM.buffer[tile_address + 1];
			int byte3 = memory.VRAM.buffer[tile_address + 16];
			int byte4 = memory.VRAM.buffer[tile_address + 17];
			int byte5 = memory.VRAM.buffer[tile_address + 32];
			int byte6 = memory.VRAM.buffer[tile_address + 33];
			int byte7 = memory.VRAM.buffer[tile_address + 48];
			int byte8 = memory.VRAM.buffer[tile_address + 49];

			int mask = 128;
			
			while ( mask > 0 )
			{
				int pixel = ( ( byte1 & mask ) != 0 ? 1 : 0 ) |
						( ( byte2 & mask ) != 0 ? 2 : 0 ) |
						( ( byte3 & mask ) != 0 ? 4 : 0 ) |
						( ( byte4 & mask ) != 0 ? 8 : 0 ) |
						( ( byte5 & mask ) != 0 ? 16 : 0 ) |
						( ( byte6 & mask ) != 0 ? 32 : 0 ) |
						( ( byte7 & mask ) != 0 ? 64 : 0 ) |
						( ( byte8 & mask ) != 0 ? 128 : 0 ) ;
				
				if ( pixel == 0 )
				{
					// Set transparent
					TileBuffer.put( GL_TRANSPARENT_COLOR );
				}
				else
				{
					TileBuffer.put( ppu.CGDATA[pixel] );
				}
				
				mask = mask >> 1;
			}
		}
    	
    	TileBuffer.rewind();
    }
    
    /**
     * Get an open texture tile index for the given tile size
     * 
     * @param	TileSizeEnum (i.e. value Tile8x8 )
     * 
     * @return	Next available texture tile index
     */
    private int getNextTilePosition(int TileSizeEnum)
    {
    	int TileSize = 1 << TileSizeEnum;
    	
    	int NextTile = NextTileSizePosition[TileSizeEnum];
    	
    	// Is the current row full?
    	if ( ( NextTile & 7 ) == 0 )
    	{   
        	// TODO: Fix over sized end tile rows
    		NextTileSizePosition[TileSizeEnum] = NextColumnPosition + TileSize;
    		NextTile = NextColumnPosition;
    		NextColumnPosition += TileSize << 3;
    	}
    	else
    	{
    		NextTileSizePosition[TileSizeEnum] = NextTile + TileSize;
    	}

    	return NextTile;
    }
    
    private void DrawSprites()
    {
    	// Process all the sprites
    	for(int s = 0; s < PPU.OAMCount; s++)
    	{
    		PPU.SOBJ sprite = ppu.OBJ[s];

    		int TileSize = SpriteSizeMap[ 8 | ppu.OBJSizeSelect];
    		
    		int sprite_address = ppu.OBJNameBase + ( (sprite.Name ) << 5 );

    		// Lookup base tile address
    		int tile_lookup = TileLookup[sprite.Palette][sprite_address >> 5];
    		
    		// If tile pixels needed decoding
    		if ( tile_lookup == 0 )
    		{
    			// Get the next available texture index position
    			tile_lookup = getNextTilePosition(TileSize);
    			TileLookup[sprite.Palette][sprite_address >> 5] = (short) ( (TileSize << 12 ) | tile_lookup );    			
    		}
    		else
    		{
    			// If tile size has changed?
    			if (TileSize != (tile_lookup >> 12))
    			{
    				System.out.format("S%03d %d != %d\n", s, TileSize, tile_lookup >> 12 );
    				tile_lookup = getNextTilePosition(TileSize);
    				TileLookup[sprite.Palette][sprite_address >> 5] = (short) ( (TileSize << 12 ) | tile_lookup );
    			}
    			else
    			{
    				tile_lookup = tile_lookup & 0xFFF;
    			}
    		}
			
    		int SpriteLength = (1 << TileSize) << 9;
    		
    		int SpritesPerRow = 32 << TileSize;
    		
    		// Loop over rows
    		for( int i = 0; i < SpriteLength; i = i + 512 )
    		{
    			// Loop over row tiles
    			for( int h = 0; h < SpritesPerRow; h = h + 32 )
    			{
	    			int tile_pixel_data_address = sprite_address + i + h;
	    			
		    		int tile_lookup_address = tile_pixel_data_address >> 5;
    			
    				int tile_palette_bit = 1 << sprite.Palette;
	    		
	    			// If tile is dirty then update existing tile
					if ( ( ppu.TileCached[PPU.TILE_4BIT][tile_lookup_address] & tile_palette_bit) == 0 )
					{
		    			DecodeTile4(tile_pixel_data_address, 128 + ( sprite.Palette << 4) );
		    			
		    			// Calculate the x and y texture coordinates from tile_lookup
		    			int texture_x = (tile_lookup & 1016) + (h >> 2); 
		    			int texture_y = ((tile_lookup & 7) << 3 ) + ((15360 & tile_lookup) >> 4) + (i >> 6);
	
		    			// Copy decoded tile into GL memory					
		    			gl.glBindTexture(GLGeneric.GL_TEXTURE_2D, TextureHandle[0]);
		    			gl.glTexSubImage2D(GLGeneric.GL_TEXTURE_2D, 0, texture_x, texture_y, 8, 8, GLGeneric.GL_RGBA, GLGeneric.GL_UNSIGNED_SHORT_5_5_5_1, TileBuffer );
		    			
		    			// Mark tile map block as clean
		    			ppu.TileCached[PPU.TILE_4BIT][tile_lookup_address] |= tile_palette_bit;
					}
    			}
	    	}
    		
    		if ( sprite.Changed )
    		{
    			sprite.Changed = false;
    			
				short zIndex = (short) ((sprite.Priority << 2) + 36);
				
				int tile_x = sprite.HPos & 0xFF;
				int tile_y = sprite.VPos & 0xFF;
				
				int TileDimension = 8 << TileSize;
				
				// Set tile vertices
				
				// Make a quad with two triangles 
				// Triangle 1 - |\
				vertexTempBuffer.rewind();
				
				// Top left
				vertexTempBuffer.put( (short) tile_x );
				vertexTempBuffer.put( (short) tile_y );
				vertexTempBuffer.put( zIndex );

				// Bottom left
				vertexTempBuffer.put( (short) tile_x );
				vertexTempBuffer.put( (short) (tile_y + TileDimension) );
				vertexTempBuffer.put( zIndex );
				
				// Bottom right
				vertexTempBuffer.put( (short) ( tile_x + TileDimension ) );
				vertexTempBuffer.put( (short) ( tile_y + TileDimension ) );
				vertexTempBuffer.put( zIndex );
				
				// Triangle 2 - \|

				// Top left
				vertexTempBuffer.put( (short) tile_x );
				vertexTempBuffer.put( (short) tile_y );
				vertexTempBuffer.put( zIndex );
		
				// Top right
				vertexTempBuffer.put( (short) ( tile_x + TileDimension ) );
				vertexTempBuffer.put( (short) tile_y );
				vertexTempBuffer.put( zIndex );

				// Bottom right
				vertexTempBuffer.put( (short) ( tile_x + TileDimension ) );
				vertexTempBuffer.put( (short) ( tile_y + TileDimension ) );
				vertexTempBuffer.put( zIndex );
								
				vertexTempBuffer.rewind();
				
				gl.glBindBuffer(GLGeneric.GL_ARRAY_BUFFER, VertexHandle[SpriteVBOIndex]);
				gl.glBufferSubData(GLGeneric.GL_ARRAY_BUFFER, s * 18 * 2, 18 * 2, vertexTempBuffer);
				
				if ( UseJogl )
				{					
					// Set tile texture coordinates
					float texture_coord_x = ((float) ((tile_lookup & 1016) >> 3)) * TextureCoordTileDimensionFloat;
					float texture_coord_y = ((float) ((tile_lookup & 7) + ((15360 & tile_lookup) >> 7))) * TextureCoordTileDimensionFloat;
					
					float texture_coord_tile_dimension = TextureCoordTileDimensionFloat * (1 << TileSize);
					
					// Based on horizontal / vertical bits goto appropriate texture coordinate mapping  
					switch (sprite.VH_Flip)
					{
					// Normal
					case 0:
						// Triangle 1 - |\

						// Top left
						texcoordTempBufferFloat.put(texture_coord_x);
						texcoordTempBufferFloat.put(texture_coord_y);

						// Bottom left
						texcoordTempBufferFloat.put(texture_coord_x);
						texcoordTempBufferFloat.put(texture_coord_y + texture_coord_tile_dimension);

						// Bottom right
						texcoordTempBufferFloat.put(texture_coord_x + texture_coord_tile_dimension);
						texcoordTempBufferFloat.put(texture_coord_y + texture_coord_tile_dimension);

						// Triangle 2 - \|

						// Top left
						texcoordTempBufferFloat.put(texture_coord_x);
						texcoordTempBufferFloat.put(texture_coord_y);

						// Top right
						texcoordTempBufferFloat.put(texture_coord_x + texture_coord_tile_dimension);
						texcoordTempBufferFloat.put(texture_coord_y);

						// Bottom right
						texcoordTempBufferFloat.put(texture_coord_x + texture_coord_tile_dimension);
						texcoordTempBufferFloat.put(texture_coord_y + texture_coord_tile_dimension);

						break;

					// Horizontal flip
					case 2:
						// Triangle 1 - |\

						// Top left
						texcoordTempBufferFloat.put(texture_coord_x);
						texcoordTempBufferFloat.put(texture_coord_y + texture_coord_tile_dimension);

						// Bottom left
						texcoordTempBufferFloat.put(texture_coord_x);
						texcoordTempBufferFloat.put(texture_coord_y);

						// Bottom right
						texcoordTempBufferFloat.put(texture_coord_x + texture_coord_tile_dimension);
						texcoordTempBufferFloat.put(texture_coord_y);

						// Triangle 2 - \|

						// Top left
						texcoordTempBufferFloat.put(texture_coord_x);
						texcoordTempBufferFloat.put(texture_coord_y + texture_coord_tile_dimension);

						// Bottom right
						texcoordTempBufferFloat.put(texture_coord_x + texture_coord_tile_dimension);
						texcoordTempBufferFloat.put(texture_coord_y + texture_coord_tile_dimension);

						// Top right
						texcoordTempBufferFloat.put(texture_coord_x + texture_coord_tile_dimension);
						texcoordTempBufferFloat.put(texture_coord_y);
						break;

					// Vertical flip
					case 1:
						// Triangle 1 - |\

						// Top left
						texcoordTempBufferFloat.put(texture_coord_x + texture_coord_tile_dimension);
						texcoordTempBufferFloat.put(texture_coord_y);

						// Bottom left
						texcoordTempBufferFloat.put(texture_coord_x + texture_coord_tile_dimension);
						texcoordTempBufferFloat.put(texture_coord_y + texture_coord_tile_dimension);

						// Bottom right
						texcoordTempBufferFloat.put(texture_coord_x);
						texcoordTempBufferFloat.put(texture_coord_y + texture_coord_tile_dimension);

						// Triangle 2 - \|

						// Top left
						texcoordTempBufferFloat.put(texture_coord_x + texture_coord_tile_dimension);
						texcoordTempBufferFloat.put(texture_coord_y);

						// Bottom right
						texcoordTempBufferFloat.put(texture_coord_x);
						texcoordTempBufferFloat.put(texture_coord_y);

						// Top right
						texcoordTempBufferFloat.put(texture_coord_x);
						texcoordTempBufferFloat.put(texture_coord_y + texture_coord_tile_dimension);
						break;

					// Horizontal and Vertical flip
					case 3:
						// Triangle 1 - |\

						// Top left
						texcoordTempBufferFloat.put(texture_coord_x + texture_coord_tile_dimension);
						texcoordTempBufferFloat.put(texture_coord_y + texture_coord_tile_dimension);

						// Bottom left
						texcoordTempBufferFloat.put(texture_coord_x + texture_coord_tile_dimension);
						texcoordTempBufferFloat.put(texture_coord_y);

						// Bottom right
						texcoordTempBufferFloat.put(texture_coord_x);
						texcoordTempBufferFloat.put(texture_coord_y);

						// Triangle 2 - \|

						// Top left
						texcoordTempBufferFloat.put(texture_coord_x + texture_coord_tile_dimension);
						texcoordTempBufferFloat.put(texture_coord_y + texture_coord_tile_dimension);

						// Bottom right
						texcoordTempBufferFloat.put(texture_coord_x);
						texcoordTempBufferFloat.put(texture_coord_y + texture_coord_tile_dimension);

						// Top right
						texcoordTempBufferFloat.put(texture_coord_x);
						texcoordTempBufferFloat.put(texture_coord_y);
						break;
					}
					
					texcoordTempBufferFloat.rewind();
					
					// Copy updated data to GL texture buffer
					gl.glBindBuffer(GLGeneric.GL_ARRAY_BUFFER, TexCoordHandle[SpriteVBOIndex]);
					gl.glBufferSubData(GLGeneric.GL_ARRAY_BUFFER, s * 12 * 4, 12 * 4, texcoordTempBufferFloat);
				}
				// OpenGL ES
				else
				{
					
				}
			}			    		
    	}
    	
		// Draw it all
    	
		// Texture coordinate points 
		gl.glBindBuffer(GLGeneric.GL_ARRAY_BUFFER, TexCoordHandle[SpriteVBOIndex] );
		gl.glTexCoordPointer(2, GLGeneric.GL_FLOAT, 0, 0 );
		
		// Setup vertices
		gl.glBindBuffer(GLGeneric.GL_ARRAY_BUFFER, VertexHandle[SpriteVBOIndex]);
		gl.glVertexPointer(3, GLGeneric.GL_SHORT, 0, 0 );
		
		gl.glDrawArrays(GLGeneric.GL_TRIANGLES, 0, SpriteVertexMapSize  );
    }

    /**
     * Reset the tile address lookup map
     */
    void ResetTileLookups()
    {
    	NextColumnPosition = 0;
    	
    	NextTileSizePosition[Tile8x8] = 0;
    	NextTileSizePosition[Tile16x16] = 0;
    	NextTileSizePosition[Tile32x32] = 0;
    	NextTileSizePosition[Tile64x64] = 0;
    	
    	// Reset tile map
    	for(int i = 0; i < TileLookup.length; i++)
    	{
    		for(int h = 0; h < TileLookup[i].length; h++)
    		{
    			TileLookup[i][h] = 0;
    		}
    	}
    }
	
	private static ShortBuffer newShortBuffer(int numElements)
	{
	    ByteBuffer bb = ByteBuffer.allocateDirect(numElements * 2);
	    bb.order(ByteOrder.nativeOrder());
	    return bb.asShortBuffer();
	 }
	
	private FloatBuffer newFloatBuffer(int numElements)
	{
	    ByteBuffer bb = ByteBuffer.allocateDirect(numElements * 4);
	    bb.order(ByteOrder.nativeOrder());
	    return bb.asFloatBuffer();		
	}
	
	private IntBuffer newIntBuffer(int numElements)
	{
	    ByteBuffer bb = ByteBuffer.allocateDirect(numElements * 4);
	    bb.order(ByteOrder.nativeOrder());
	    return bb.asIntBuffer();		
	}
}
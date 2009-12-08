
package snesjx;

import junit.framework.TestCase;

public class CMemoryTest extends TestCase {

	private Memory mem;
	
	protected void setUp() throws Exception {
		super.setUp();
		Globals.setUp();
		mem = Globals.globals.memory;
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		
	}

	public void testLoadROM() {
	
		try {
			
			// Load the ROM
			mem.LoadROM( "unittest/roms/TestRom.smc" );
			
			// Attempt to the Initalize the ROM into memory
			mem.InitROM();
			
			/*
			assertEquals( "SUPER MARIOWORLD", mem.ROMName.getString() );
			assertEquals( "ffffffff ffffffff ffffffff ffffffff ", mem.ROMId.getHexString(0 , 4) );
			assertEquals( "B19ED489" , Integer.toHexString( mem.ROMCRC32 ).toUpperCase() );
			assertEquals( "1", mem.CompanyId );
			assertEquals( true, mem.isChecksumOK );
			assertEquals( "LoROM", mem.GetMapType() );
			assertEquals( "4Mbits", mem.GetROMSizeMB() );
			assertEquals( "ROM+RAM+BAT", mem.CartridgeChip() );
			assertEquals( 0x20, mem.ROMSpeed & ~0x10 );
			assertEquals( "NTSC", mem.GetVideoMode() );
			assertEquals( "2KB", mem.StaticRAMSize() );	
			*/
			
		}catch ( Exception e) {
			System.out.println( "Caught Exception '" + e.getMessage() + "'" );
			e.printStackTrace();
			fail("Failed to load ROM");
		}
	}
	
	public void testAccumulate() {
		
		// Adds the Decimal values of the bytes, so 1234 is actually 49,50,51,52
		//assertEquals( 202, mem.Accumulate( new ByteBuffer( "1234".getBytes() ).getOffsetBuffer(0), 4 ) );
	}
	
	public void testTruncate() {
		
		// Test in hex, with a funny char at the end
		byte[] bytesMario = { 0x53, 0x55, 0x50, 0x45, 0x52, 0x20, 0x4d, 0x41, 0x52, 0x49, 0x4f, 0x57, 0x4f, 0x52, 0x4c, 0x44, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x02 };

		//assertEquals( 23, CMemory.strlen( bytesMario ) );
		//assertEquals( "SUPER MARIOWORLD", new String( mem.Truncate( new ByteBuffer( bytesMario ), 23 ).array()) );

	}
}

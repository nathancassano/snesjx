package snesjx;

import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class SCPUStateTest {

	static CPU CPU;
	static Memory mem;
	
	@BeforeClass
	public static void oneTimeSetUp() {
		
		try {
			// Get the object
			Globals.setUp();

			CPU = Globals.globals.cpu;
			mem = Globals.globals.memory;
			
			// Load the test ROM
			mem.LoadROM( "unittest/roms/TestRom.smc" );
			mem.InitROM();
			
		}catch ( Exception e) {
			System.out.println( "Caught Exception '" + e.getMessage() + "'" );
			e.printStackTrace();
			fail("Failed to load ROM");
		}
	}
	
	@Test
	public void testUnPackStatus() {
		
		CPU.UnpackStatus();
		assertEquals( 0, CPU._Carry);
		assertEquals( 0, CPU._Negative );
		assertEquals( 0, CPU._Overflow );
		assertEquals( 1, CPU._Zero );
		
	}
	
}

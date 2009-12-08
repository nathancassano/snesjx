package snesjx;


import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class OpCodeTest {


	static CPU cpu;
	static Memory mem;

	static Globals globals;
	
	@BeforeClass
	public static void oneTimeSetUp() {
		
		try {
			// Get the object
			Globals.setUp();
			cpu = Globals.globals.cpu;
			mem = Globals.globals.memory;
			globals = Globals.globals;
			
			// Load the test ROM
			mem.LoadROM( "unittest/roms/UnitTest.smc" );
			mem.InitROM();
			
			cpu.ResetCPU();
			
		}catch ( Exception e) {
			System.out.println( "Caught Exception '" + e.getMessage() + "'" );
			e.printStackTrace();
			fail("Failed to load ROM");
		}
	}


	@Test 
	public void testADC8()
	{
		/*
		// Should be Zero
		assertEquals( 0, cpu.A_L() );
		
		// Add 5 to the AL register
		cpu.ADC8( 5 );
		assertEquals( 5, cpu.A_L() );
		
		// Add 8 to the register
		cpu.ADC8( 8 );
		assertEquals( 13, cpu.A_L() );
		assertEquals( false , cpu.CheckCarry() );
		
		// Add past our max value ( 0xFF = 255 )
		cpu.ADC8( 255 );
		assertEquals( 12, cpu.A_L() );
		assertEquals( true , cpu.CheckCarry() );
		
		// Reset for next test
		cpu.ClearCarry();
		assertEquals( false , cpu.CheckCarry() );
		cpu.A_L(0);
		assertEquals( 0, cpu.A_L() );
		assertEquals( 0, cpu.A );
		
		cpu.ADC8( 255 );
		assertEquals( 255, cpu.A_L() );
		
		// Add pass our max value
		cpu.ADC8( 255 );
		assertEquals( 254, cpu.A_L() );
		assertEquals( true , cpu.CheckCarry() );
		assertEquals( false , cpu.CheckOverflow() );
		
		cpu.ClearCarry();
		assertEquals( false , cpu.CheckCarry() );
		cpu.A_L(0);
		assertEquals( 0, cpu.A_L() );
		assertEquals( 0, cpu.A );
		*/
	}
	
	@Test
	public void testADC8Decimal()
	{
		/*
		cpu.SetDecimal();
		
		assertEquals( true, cpu.CheckDecimal() );
		
		// Should be Zero
		assertEquals( 0, cpu.A_L() );

		// Add 5 to the AL register
		cpu.ADC8( 5 );
		assertEquals( 0x05, cpu.A_L() );
		
		// Add 8 to the register
		cpu.ADC8( 8 );
		assertEquals( 0x13, cpu.A_L() );
		assertEquals( false , cpu.CheckCarry() );
		
		// Add past our max value ( 0xFF = 255 )
		cpu.ADC8( 255 );
		assertEquals( 0x78, cpu.A_L() );
		assertEquals( true , cpu.CheckCarry() );
		
		// Reset for next test
		cpu.ClearCarry();
		assertEquals( false , cpu.CheckCarry() );
		cpu.A_L(0);
		assertEquals( 0, cpu.A_L() );
		assertEquals( 0, cpu.A );
		
		cpu.ADC8( 255 );
		assertEquals( 0x65, cpu.A_L() );
		
		// Add pass our max value
		cpu.ADC8( 255 );
		assertEquals( 0xCB, cpu.A_L() );
		assertEquals( true , cpu.CheckCarry() );
		assertEquals( false , cpu.CheckOverflow() );
		
		cpu.ClearCarry();
		assertEquals( false , cpu.CheckCarry() );
		cpu.A_L(0);
		assertEquals( 0, cpu.A_L() );
		assertEquals( 0, cpu.A );
		*/
	}

	@Test
	public void testImmediate16() {
		assertEquals( 0x8000, cpu.PCw);
        cpu.PCw = 0x8000;
        assertEquals( 0x8000, cpu.PCw);
        cpu.PCBase.get8Bit(0);
        assertEquals( 0x0001, cpu.PCBase.get16Bit( cpu.PCw ) );
        	

        // Read the first 8bit instruction and incr the program counter
        assertEquals( 0x0001, cpu.Immediate16SlowRead() );
        // Open bus contains the Read instruction
        assertEquals( 0x00, globals.OpenBus );

        // Program Counter points to the next instruction ( +2 bytes )
        assertEquals( 0x8002, cpu.PCw );
        assertEquals( 0xFFFF, cpu.PCBase.get16Bit( cpu.PCw ) );

	}

	@Test
	public void testImmediate8() {
	   
        cpu.PCw = 0x8004;
        assertEquals( 0x8004, cpu.PCw );
        assertEquals( 0x9C, cpu.PCBase.get8Bit( cpu.PCw ) );
        // Read the first 8bit instruction and incr the program counter
        /*
        assertEquals( 0x9C, cpu.Immediate8Slow( WDC_65c816.READ ) );
        // Open bus contains the Read instruction
        assertEquals( 0x9C, globals.OpenBus );

        // Program Counter points to the next instruction
        assertEquals( 0x8005, cpu.PCw() );
        assertEquals( 0x0C, cpu.PCBase.get8Bit(cpu.PCw() ) );
        */

	}
	
	/*
	@Test
	public void testRelative() {

        cpu.PCw( 0x8006 );
        assertEquals( 0x8006,  cpu.PCw() );
		assertEquals( 0x02, cpu.PCBase.get8Bit( cpu.PCw() ) );

        // 8 Bit Read and Return the address to JUMP to Relative the the current address
        
        // Return the address +2 Bytes
        assertEquals( 0x8009, cpu.Relative( WDC_65c816.JUMP ) );
        assertEquals( 0x02, globals.OpenBus);

        cpu.PCw( 0x8009 );
        assertEquals( 0x8009,  cpu.PCw() );
        assertEquals( 0xFE, cpu.PCBase.get8Bit( cpu.PCw() ) );

        // Return the address -2 Bytes 
        assertEquals( 0x8008, cpu.Relative( WDC_65c816.JUMP ) );
				
	}

	@Test
	public void testRelativeLong() {

        cpu.PCw( 0x8006 );
        assertEquals( 0x8006,  cpu.PCw() );
		assertEquals( 0x02, cpu.PCBase.get8Bit( cpu.PCw() ) );

        // 16 Bit Read and Return the address to JUMP to Relative the the current address
        
        // Return the address +2 Bytes
        assertEquals( 0x800A, cpu.RelativeLong( WDC_65c816.JUMP ) );
        assertEquals( 0x00, globals.OpenBus);

        cpu.PCw( 0x8009 );
        assertEquals( 0x8009,  cpu.PCw() );
        assertEquals( 0xFE, cpu.PCBase.get8Bit( cpu.PCw() ) );
				
        assertEquals( 0x8109, cpu.RelativeLong( WDC_65c816.NONE ) );

	}
	
	@Test
	public void testAbsoluteIndexedIndirect() {

        cpu.PCw( 0x800B );
        assertEquals( 0x800B,  cpu.PCw() );

		assertEquals( 0x8000, cpu.InspectWord( cpu.PCw() ) );
        cpu.X.W = 13;

        // Go to the address 0x8000 then add 13 ( X=13 ) and return the resulting address
        assertEquals( 0x800D, cpu.AbsoluteIndexedIndirectSlow( WDC_65c816.JUMP ) );
        
        cpu.PCw( 0x800B );
        assertEquals( 0x800D, cpu.AbsoluteIndexedIndirectSlow( WDC_65c816.JSR ) );

	}

	@Test
	public void testAbsoluteIndirectLong() {

        cpu.PCw( 0x8010 );
        assertEquals( 0x8010,  cpu.PCw() );

		assertEquals( 0x8012, cpu.InspectWord( cpu.PCw() ) );

        // Go to the address 0x8012 and return the address 
        assertEquals( 0x800012, cpu.AbsoluteIndirectLong( WDC_65c816.READ ) );
        
	}
	
	@Test
	public void testAbsoluteIndirect() {

        cpu.PCw( 0x8010 );
        assertEquals( 0x8010,  cpu.PCw() );

		assertEquals( 0x8012, cpu.InspectWord( cpu.PCw() ) );

        // Go to the address 0x8012 and return the address 
        assertEquals( 0x0012, cpu.AbsoluteIndirect( WDC_65c816.READ ) );
        
	}

	@Test
	public void testAbsolute() { // a

        cpu.PCw( 0x8010 );
        assertEquals( 0x8010,  cpu.PCw() );

		assertEquals( 0x8012, cpu.InspectWord( cpu.PCw() ) );

        // Go to the address at the Program Counter
        assertEquals( 0x8012, cpu.Absolute( WDC_65c816.READ ) );
        
	}

	@Test
	public void testAbsoluteLong() { // l

        cpu.PCw( 0x8012 );
        assertEquals( 0x8012,  cpu.PCw() );

		assertEquals( 0x0012, cpu.InspectWord( cpu.PCw() ) );

        // Go to the address at the Program Counter
        assertEquals( 0x800012, cpu.AbsoluteLong( WDC_65c816.READ ) );
        
	}

	@Test
	public void testDirect() { // d

        cpu.PCw( 0x8000 );
        assertEquals( 0x8000,  cpu.PCw() );

		assertEquals( 0x01, cpu.InspectByte( cpu.PCw() ) );
        cpu.D.W = 10; 

        // Go to the address at the Program Counter
        assertEquals( 0x0B, cpu.Direct( WDC_65c816.READ ) );
        
	}

	@Test
	public void testDirectIndirectE1() { // (d)

        cpu.PCw( 0x8000 );
        assertEquals( 0x8000,  cpu.PCw() );

		assertEquals( 0x01, cpu.InspectByte( cpu.PCw() ) );

        cpu.PutWord( 0x0001, 0x0B );
        cpu.D.W = 10; 

        // Add the value at 0x8000 ( 0x01 ) with the value in Register D
        // Then return the 16bit value at the address 
        assertEquals( 0x0001, cpu.DirectIndirectE1( WDC_65c816.READ ) );
        
	}

	@Test
	public void testDirectIndirectE0() { // (d)

        cpu.PCw( 0x8000 );
        assertEquals( 0x8000,  cpu.PCw() );

		assertEquals( 0x01, cpu.InspectByte( cpu.PCw() ) );

        cpu.PutWord( 0x0001, 0x0B );
        cpu.D.W = 10; 

        // Add the value at 0x8000 ( 0x01 ) with the value in Register D
        // Then return the 16bit value at the address 
        assertEquals( 0x0001, cpu.DirectIndirectE0( WDC_65c816.READ ) );
        
	}

	@Test
	public void testDirectIndirectIndexedE0X0() { // (d), Y

        cpu.PCw( 0x8000 );
        assertEquals( 0x8000,  cpu.PCw() );

		assertEquals( 0x01, cpu.InspectByte( cpu.PCw() ) );

        // Setup the Stack for test
        cpu.PutWord( 0x0001, 0x0B );

        cpu.D.W = 10; 
        cpu.Y.W = 0x09; 

        // Add the value at 0x8000 ( 0x01 ) with the value in Register D
        // Then return the 16bit value at the address, And add the Value 
        // in Register Y to the address
        assertEquals( 0x0A, cpu.DirectIndirectIndexedE0X0( WDC_65c816.READ ) );
        
	}

	@Test
	public void testDirectIndirectLong() { // [d]

        cpu.PCw( 0x8000 );
        assertEquals( 0x8000,  cpu.PCw() );

		assertEquals( 0x01, cpu.InspectByte( cpu.PCw() ) );

        // Setup the Stack for test
        cpu.PutWord( 0x0001, 0x0B );
        cpu.PutByte( 0xFF, 0x0D );

        cpu.D.W = 10; 
        cpu.Y.W = 0;

        // Add the value at 0x8000 ( 0x01 ) with the value in Register D
        // Then return the 16bit value at the address 
        assertEquals( 0xFF0001, cpu.DirectIndirectLong( WDC_65c816.READ ) );
        assertEquals( 0xFF, globals.OpenBus );
        
	}

	@Test
	public void testDirectIndirectIndexedLong() { // [d], Y

        cpu.PCw( 0x8000 );
        assertEquals( 0x8000,  cpu.PCw() );

		assertEquals( 0x01, cpu.InspectByte( cpu.PCw() ) );

        // Setup the Stack for test
        cpu.PutWord( 0x0001, 0x0B );
        cpu.PutByte( 0x00, 0x0D );

        cpu.D.W = 10; 
        cpu.Y.W = 5;

        assertEquals( 0x000006, cpu.DirectIndirectLong( WDC_65c816.READ ) );
        assertEquals( 0x00, globals.OpenBus );
        
	}

	@Test
	public void testDirectIndexedYE0() { // d, Y

        cpu.PCw( 0x8000 );
        assertEquals( 0x8000,  cpu.PCw() );

		assertEquals( 0x01, cpu.InspectByte( cpu.PCw() ) );

        cpu.D.W = 10; 
        cpu.Y.W = 5;

        // Add the value at 0x8000 ( 0x01 ) with the value in Register D
        // Then add the Value in register Y
        assertEquals( 0x10, cpu.DirectIndexedYE0( WDC_65c816.READ ) );
        
	}

	@Test
	public void testDirectIndexedYE1_DRegister() { // d, Y

        cpu.PCw( 0x8000 );
        assertEquals( 0x8000,  cpu.PCw() );

		assertEquals( 0x01, cpu.InspectByte( cpu.PCw() ) );

        cpu.D.W = 10; 
        cpu.Y.W = 5;

        // Add the value at 0x8000 ( 0x01 ) with the value in Register D
        // Then add the Value in register Y
        assertEquals( 0x10, cpu.DirectIndexedYE1( WDC_65c816.READ ) );
        
	}

	@Test
	public void testDirectIndexedYE1() { // d, Y

        cpu.PCw( 0x8000 );
        assertEquals( 0x8000,  cpu.PCw() );

		assertEquals( 0x01, cpu.InspectByte( cpu.PCw() ) );

        cpu.D.W = 0; 
        cpu.Y.W = 5;

        // Add the value at 0x8000 ( 0x01 ) Then add the Value in register Y
        assertEquals( 0x06, cpu.DirectIndexedYE0( WDC_65c816.READ ) );
        
	}

	@Test
	public void testDirectIndexedXE0() { // d, X

        cpu.PCw( 0x8000 );
        assertEquals( 0x8000,  cpu.PCw() );

		assertEquals( 0x01, cpu.InspectByte( cpu.PCw() ) );

		assertEquals( 0x01, cpu.InspectByte( 10 + 0x01 ) );

        cpu.D.W = 0; 
        cpu.Y.W = 0;
        cpu.X.W = 5;

        // Add the value at 0x8000 ( 0x01 ) Then add the Value in register Y
        // Then add the Value in register X
        assertEquals( 0x06, cpu.DirectIndexedXE0( WDC_65c816.READ ) );
        
	}

	@Test
	public void testDirectIndexedXE1_DRegister() { // d, X

        cpu.PCw( 0x8000 );
        assertEquals( 0x8000,  cpu.PCw() );

		assertEquals( 0x01, cpu.InspectByte( cpu.PCw() ) );

		assertEquals( 0x01, cpu.InspectByte( 10 + 0x01 ) );

        cpu.D.W = 10; 
        cpu.Y.W = 0;
        cpu.X.W = 5;

        // Add the value at 0x8000 ( 0x01 ) Then add the Value in register Y
        // Then add the Value in register X
        assertEquals( 0x10, cpu.DirectIndexedXE1( WDC_65c816.READ ) );
        
	}

	@Test
	public void testDirectIndexedXE1() { // d, X

        cpu.PCw( 0x8000 );
        assertEquals( 0x8000,  cpu.PCw() );

		assertEquals( 0x01, cpu.InspectByte( cpu.PCw() ) );

		assertEquals( 0x01, cpu.InspectByte( 10 + 0x01 ) );

        cpu.D.W = 0; 
        cpu.Y.W = 0;
        cpu.X.W = 5;

        // Add the value at 0x8000 ( 0x01 ) Then add the Value in register Y
        // Then add the Value in register X
        assertEquals( 0x06, cpu.DirectIndexedXE1( WDC_65c816.READ ) );
        
	}

	@Test
	public void testDirectIndexedIndirectE0() { // ( d, X )

        cpu.PCw( 0x8000 );
        assertEquals( 0x8000,  cpu.PCw() );

		assertEquals( 0x01, cpu.InspectByte( cpu.PCw() ) );

        cpu.PutWord( 0x00001, 0x06 );
        cpu.D.W = 0; 
        cpu.Y.W = 0;
        cpu.X.W = 5;

        // Add the value at 0x8000 ( 0x01 ) Then add the Value in register Y
        // then return the value at that address
        assertEquals( 0x0001, cpu.DirectIndexedIndirectE0( WDC_65c816.READ ) );
        
	}

	@Test
	public void testAbsoluteIndexedXX0() { // a, X

        cpu.PCw( 0x8010 );
        assertEquals( 0x8010,  cpu.PCw() );

		assertEquals( 0x8012, cpu.InspectWord( cpu.PCw() ) );

        cpu.D.W = 0; 
        cpu.Y.W = 0;
        cpu.X.W = 5;

        assertEquals( 0x8017, cpu.AbsoluteIndexedXX0( WDC_65c816.READ ) );
        
	}

	@Test
	public void testAbsoluteIndexedYX0() { // a, Y

        cpu.PCw( 0x8010 );
        assertEquals( 0x8010,  cpu.PCw() );

		assertEquals( 0x8012, cpu.InspectWord( cpu.PCw() ) );

        cpu.D.W = 0; 
        cpu.Y.W = 5;
        cpu.X.W = 0;

        assertEquals( 0x8017, cpu.AbsoluteIndexedYX0( WDC_65c816.READ ) );
        
	}

	@Test
	public void testAbsoluteLongIndexedX() { // l, X

        cpu.PCw( 0x8010 );
        assertEquals( 0x8010,  cpu.PCw() );

		assertEquals( 0x8012, cpu.InspectWord( cpu.PCw() ) );

        cpu.D.W = 0; 
        cpu.Y.W = 0;
        cpu.X.W = 5;

        assertEquals( 0x800017, cpu.AbsoluteLongIndexedX( WDC_65c816.READ ) );
        
	}

	@Test
	public void testStackRelative() { // d, S

        cpu.PCw( 0x8010 );
        assertEquals( 0x8010,  cpu.PCw() );

		assertEquals( 0x0001, cpu.InspectWord( cpu.PCw() ) );

        cpu.D.W = 0; 
        cpu.Y.W = 0;
        cpu.X.W = 5;
        assertEquals( 0x01FF, cpu.S.W );

        assertEquals( 0x200, cpu.StackRelative( WDC_65c816.READ ) );
        
	}

	@Test
	public void testStackRelativeIndirectIndexed() { // ( d, S ) , Y

        cpu.PCw( 0x8000 );
        assertEquals( 0x8000,  cpu.PCw() );

		assertEquals( 0x0001, cpu.InspectWord( cpu.PCw() ) );

        cpu.PutWord( 0x0001, 0x200 );
        cpu.D.W = 0; 
        cpu.Y.W = 5;
        cpu.X.W = 0;
        assertEquals( 0x01FF, cpu.S.W );

        assertEquals( 0x0006, cpu.StackRelativeIndirectIndexed( WDC_65c816.READ ) );
        
	}

	@Test
	public void testGetSet() {
		
		assertEquals( 0x00, cpu.GetByte( 0xFFFC ) );
		
		assertEquals( 0x80, cpu.GetByte( 0xFFFD ) );
		
		assertEquals( 0x8000, cpu.GetWord( 0xFFFC ) );
	}
	
	@Test
	public void testRegisters() {
		
		Registers reg = new Registers();
		
		reg.PBPC( 0xFFFFFFFF );
		assertEquals( 0xFFFFFFFF, reg.PBPC() );
		
		reg.PCw( 0x2222 );
		assertEquals( 0xFFFF2222, reg.PBPC() );
		assertEquals( 0x2222, reg.PCw() );
		
		// Reset for next test
		reg.PBPC( 0x0000FFFF );
		
		reg.PCl( 0x77 );
		
		assertEquals( 0x0000FF77, reg.PBPC() );
		assertEquals( 0x77, reg.PCl() );
		
		reg.PCh( 0x22 );
		assertEquals( 0x00002277, reg.PBPC() );
		assertEquals( 0x22, reg.PCh() );
		
		reg.PB( 0x33 );
		assertEquals( 0x00332277, reg.PBPC() );
		assertEquals( 0x33, reg.PB() );
		
		reg.A.W = 0xFFFF;
		
		reg.A_L( 0x33 );
		assertEquals( 0xFF33, reg.A.W );
		assertEquals( 0x0033, reg.A_L() );
		
		reg.A.H( 0x22 );
		assertEquals( 0x2233, reg.A.W );
		assertEquals( 0x0022, reg.A.H() );
		
	}
	*/
}

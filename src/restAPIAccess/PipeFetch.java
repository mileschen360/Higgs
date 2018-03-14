package restAPIAccess;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class PipeFetch 
{
	String code;
	
	PipeFetch() throws IOException
	{
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        String input;
        code = "";
        while ((input = br.readLine()) != null) 
        {
        	code = code + input;
        }
	}
	
	public String fetchCode()
	{
		return code;
	}
}

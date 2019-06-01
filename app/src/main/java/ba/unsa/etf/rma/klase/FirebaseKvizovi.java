package ba.unsa.etf.rma.klase;

import android.support.v7.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class FirebaseKvizovi extends AppCompatActivity {

    public static String streamToStringConversion(InputStream is){
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is));
        StringBuilder stringBuilder = new StringBuilder();
        String line = null;
        try{
            while( (line = bufferedReader.readLine()) != null )
                stringBuilder.append(line + "\n");
        }
        catch(IOException e) { } finally {
            try{
                is.close();
            }
            catch (IOException e){

            }

        }
        return stringBuilder.toString();
    }
}

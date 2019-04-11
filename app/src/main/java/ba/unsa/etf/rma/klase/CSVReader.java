package ba.unsa.etf.rma.klase;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class CSVReader {

    private InputStream inputStream;

    public CSVReader(InputStream inputStream){
        this.inputStream = inputStream;
    }

    public ArrayList<String[]> read(){
        ArrayList<String []> resultList = new ArrayList<>();
        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
        try{
            String csvLine;
            while( (csvLine = bufferedReader.readLine()) != null ){
                String[] row = csvLine.split(",");
                if( !csvLine.equals("") )
                     resultList.add(row);
            }
        }
        catch (Exception e){
            e.printStackTrace();
        } finally {
            try{
                inputStream.close();
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
        return resultList;
    }
}

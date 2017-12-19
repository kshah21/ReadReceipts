package com.kshah21.readreceipts.OCR;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Kshah21 on 8/21/2017.
 */

public class OCR {
    private TessBaseAPI tessAPI;
    private Context context;

    private final String TESS_DATA = "/tessdata";
    private final String DATA_PATH = Environment.getExternalStorageDirectory() + "/ReadReceipts";

    /**
     * Constructor
     */
    public OCR(Context context){
        this.context = context;
        setUpTess();
    }

    /**
     * Executes OCR on bitmap and obtains total
     * from the result of the OCR
     */
    public String doOCR(Bitmap bitmap){
        String result = OCR(bitmap);
        String total = obtainTotal(result);
        return total;
    }

    /**
     * Copies over tess training data onto phone memory
     */
    private void setUpTess(){
        System.out.println("Set Up Tess");
        try{
            File dir = new File(DATA_PATH + TESS_DATA);
            if(!dir.exists()){
                dir.mkdir();
            }
            String fileList[] = context.getAssets().list("");
            for(String fileName : fileList){
                String pathToDataFile = DATA_PATH+TESS_DATA+"/"+fileName;
                if(!(new File(pathToDataFile)).exists()){
                    InputStream in = context.getAssets().open(fileName);
                    OutputStream out = new FileOutputStream(pathToDataFile);
                    byte [] buff = new byte[1024];
                    int len ;
                    while(( len = in.read(buff)) > 0){
                        out.write(buff,0,len);
                    }
                    in.close();
                    out.close();
                }
            }
        } catch (Exception e) {
        }
    }

    /**
     * Processes provided bitmap and produces text equivalent
     */
    private String OCR(Bitmap bitmap){
        String upLetters = "ACBDEFGHIJKLMNOPQRSTUVWXYZ";
        String downLetters="acbcdefghijklmnopqrstuvwxyz";
        String numbers="0123456789";
        String symbols="=*$.- ";
        String whitelist = upLetters + downLetters + numbers + symbols;

        tessAPI = new TessBaseAPI();
        tessAPI.init(DATA_PATH,"eng");
        tessAPI.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST,whitelist);
        tessAPI.setImage(bitmap);
        String result = tessAPI.getUTF8Text();
        tessAPI.end();

        return result;
    }

    /**
     * Checks OCR Result for varied regexs which
     * represent Total Amount Spent
     */
    private String obtainTotal(String result){
        //Will not work if total is preceded by a number
        //Secondary search should be for subtotal + tax!
        String regex = "(?<![\\w\\d])(?i)(total|tender|((t)(0|o)(t)(a|4)(l|1|i)))(\\s{0,4})(\\${0,1})(\\d{1,})(\\.{0,1})(\\d{0,2})"; // --> For t01al or some misread
        //String regex = "(?<![\\w\\d])(?i)(total|tender)(\\s{0,3})(\\${0,1})(\\d{1,})(\\.{0,1})(\\d{0,2})";
        Pattern pattern = Pattern.compile(regex);
        Matcher match = pattern.matcher(result);

        boolean foundTotal = match.find();
        if(!foundTotal){
            System.out.println("Total Not Found");
            return null;
        }
        int index = match.start();
        String total = match.group();

        return total;
    }
}

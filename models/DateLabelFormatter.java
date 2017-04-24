package models;
 
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.swing.JFormattedTextField.AbstractFormatter;
 
public class DateLabelFormatter extends AbstractFormatter {

	private static final long serialVersionUID = -4024791896054952894L;

    private String datePattern = "dd-MM-yyyy";
	private String dateHourPattern = "dd-MM-yy-hh-mm";
    private SimpleDateFormat dateFormatter = new SimpleDateFormat(datePattern);
    private SimpleDateFormat dateHourFormatter = new SimpleDateFormat(dateHourPattern);
    @Override
    public Object stringToValue(String text) throws ParseException {
        return dateFormatter.parseObject(text);
    }
 
    @Override
    public String valueToString(Object value) throws ParseException {
        if (value != null) {
            Calendar cal = (Calendar) value;
            return dateFormatter.format(cal.getTime());
        }
         
        return "";
    }

    /*
     *  GETTERS & SETTERS
     */
    public String getDatePattern() {
        return datePattern;
    }

    public void setDatePattern(String datePattern) {
        this.datePattern = datePattern;
    }

    public String getDateHourPattern() {
        return dateHourPattern;
    }

    public void setDateHourPattern(String dateHourPattern) {
        this.dateHourPattern = dateHourPattern;
    }

}
package ca.etsmtl.taf.selenium.payload.requests;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

@Data
public class SeleniumResponse implements Serializable {
    public int case_id;
    public String caseName;
    public List<SeleniumAction> seleniumActions;
    public boolean success;
    public long timestamp;
    public long duration;
    public String output;
}

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;


public class YamlTest {
	public static void main(String[] args) throws Exception {
		org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();
		InputStream input = new FileInputStream(new File("egypt.yml"));
		@SuppressWarnings("unchecked")
		Map<String, Object> map = (Map<String, Object>) yaml.load(input);
		input.close();
		// Extract the bird species entry:
		@SuppressWarnings("unchecked")
		ArrayList<Object> demeList = (ArrayList<Object>) map.get("BirdSpecies");
		for (int deme = 0; (deme < demeList.size()); deme++) {
			@SuppressWarnings("unchecked")
			ArrayList<Object> speciesList = (ArrayList<Object>) demeList.get(deme);
			int demeNum = -1;  // To be filled-in below.
			for (int i = 0; (i < speciesList.size()); i++) {
				@SuppressWarnings("unchecked")
				Map<String, Object> speciesInfo = (Map<String, Object>) speciesList.get(i);
				// This entry can either be a deme entry to species information.
				if (speciesInfo.get("deme") != null) {
					demeNum = (int) speciesInfo.get("deme");
				} else {
					// Species information entry
					final String birdName   = (String) speciesInfo.get("Name");
					final int    population = (int)    speciesInfo.get("Population");
					final double lifeSpan   = (double) speciesInfo.get("Life-span");
					System.out.println("Name: " + birdName +  ", Population: " + population +
							", Life-span: " + lifeSpan + ", deme: " + demeNum);
				}
			}
		}
	}
}

package entity;

import java.util.Arrays;
import java.util.List;

public class Parameter {
	public String root_path;
	public String output_add;
	public Dataset dataset;
	public String file_add;
	public String fd_add;
	public String fd_nonredundant_cover_add;
	public String fd_reduced_cover_add;
	public String fd_canonical_cover_add;
	public String fd_minimal_cover_add;
	public String fd_reduced_minimal_cover_add;
	public String fd_optimal_cover_add;
	public String fd_keyfd_add;
	public String fd_nonredundant_keyfd_cover_add;
	public String fd_reduced_keyfd_cover_add;
	public String fd_canonical_keyfd_cover_add;
	public String fd_minimal_keyfd_cover_add;
	public String fd_reduced_minimal_keyfd_cover_add;
	public String fd_optimal_keyfd_cover_add;
	public List<String> FDCoverTypeList;
	public List<String> KeyFDCoverTypeList;
	
	public Parameter() {}
	
	public Parameter(Dataset dataset) {
		this.root_path = "C:\\Users\\zzha969\\OneDrive - The University of Auckland\\Desktop\\PhD\\Armstrong interviews(FD)";
		
		this.dataset = dataset;
		
		this.output_add = root_path + "\\Exp Results\\results.csv";
		
		this.file_add = root_path + "\\Dataset\\"+dataset.name+".csv";
		
		
		String fdRootPath = null;
		switch(dataset.DataType) {
			case COMPLETE :
				fdRootPath = root_path + "\\FD\\FD on Complete";
				break;
			case NULL_EQUALITY :
				fdRootPath = root_path + "\\FD\\FD on Incomplete\\FD on NULL EQUALITY";
				break;
			case NULL_UNCERTAINTY :
				fdRootPath = root_path + "\\FD\\FD on Incomplete\\FD on NULL UNCERTAINTY";
				break;
		}
		
		this.FDCoverTypeList = Arrays.asList("original","nonredundant","reduced","canonical","minimal","reduced minimal","optimal");
		this.fd_add = fdRootPath + "\\FD\\"+dataset.name+".json";
		this.fd_nonredundant_cover_add = fdRootPath + "\\FDCover\\nonredundant\\" + dataset.name+"(nonredundant).json";
		this.fd_reduced_cover_add = fdRootPath + "\\FDCover\\reduced\\" + dataset.name+"(reduced).json";
		this.fd_canonical_cover_add = fdRootPath + "\\FDCover\\canonical\\" + dataset.name+"(canonical).json";
		this.fd_minimal_cover_add = fdRootPath + "\\FDCover\\minimal\\" + dataset.name+"(minimal).json";
		this.fd_reduced_minimal_cover_add = fdRootPath + "\\FDCover\\reduced minimal\\" + dataset.name+"(reduced minimal).json";
		this.fd_optimal_cover_add = fdRootPath + "\\FDCover\\optimal\\" + dataset.name+"(optimal).json";
		
		this.KeyFDCoverTypeList = Arrays.asList("original keyfd","nonredundant keyfd","reduced keyfd","canonical keyfd","minimal keyfd","reduced minimal keyfd","optimal keyfd");
		this.fd_keyfd_add = fdRootPath + "\\KeyFD\\"+dataset.name+"(keyfd).json";
		this.fd_nonredundant_keyfd_cover_add = fdRootPath + "\\KeyFDCover\\nonredundant\\" + dataset.name+"(nonredundant-keyfd).json";
		this.fd_reduced_keyfd_cover_add = fdRootPath + "\\KeyFDCover\\reduced\\" + dataset.name+"(reduced-keyfd).json";
		this.fd_canonical_keyfd_cover_add = fdRootPath + "\\KeyFDCover\\canonical\\" + dataset.name+"(canonical_keyfd).json";
		this.fd_minimal_keyfd_cover_add = fdRootPath + "\\KeyFDCover\\minimal\\" + dataset.name+"(minimal-keyfd).json";
		this.fd_reduced_minimal_keyfd_cover_add = fdRootPath + "\\KeyFDCover\\reduced minimal\\" + dataset.name+"(reduced minimal-keyfd).json";
		this.fd_optimal_keyfd_cover_add = fdRootPath + "\\KeyFDCover\\optimal\\" + dataset.name+"(optimal-keyfd).json";
		
	}
	
}

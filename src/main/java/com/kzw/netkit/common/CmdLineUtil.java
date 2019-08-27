package com.kzw.netkit.common;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CmdLineUtil {
	private static final String OPT_DELIMITER = "  ";
	
	private String appName = null;
	
	// 支持的参数list
	private List<Opt> optList = new ArrayList<>();
	
	// 命令行参数list
	private List<String> argList = new ArrayList<>();
	
	//说明备注
	private List<String> commentsList = new ArrayList<>();

	public CmdLineUtil(String[] args) {
		this("App.jar", args);
	}

	public CmdLineUtil(String appName, String[] args) {
		this.appName = appName;
		for (int i = 0; i < args.length; i++) {
			argList.add(args[i]);
		}
		optList.add(new Opt("--help", "-h", "", "显示帮助"));
	}
	
	public void validateArgs() {
		for(String arg : argList) {
			boolean isContain = false;
			if(!arg.startsWith("-")) {
				continue;
			}
			for (Opt opt : optList) {
				if(arg.equals(opt.getOptName())
						|| arg.equals(opt.getShortName())) {
					isContain = true;
					continue;
				}
			}
			if(!isContain) {
				throw new IllegalArgumentException("未知参数 : "+arg);
			}
		}
	}
	
	public String getOptShortName(String optName) {
		for (Opt opt : optList) {
			if(optName.equals(opt.getOptName())) {
				return opt.getShortName();
			}
		}
		return null;
	}

	public boolean hasOption(String optName) {
		String shortOpt = getOptShortName(optName);
		return argList.contains(optName) || argList.contains(shortOpt);
	}

	public List<String> getMultiOptionValue(String optName) {
		List<String> tarList = new ArrayList<>();
		String shortName = getOptShortName(optName);
		for (int i = 0; i < argList.size(); i++) {
			if(optName.equals(argList.get(i)) || shortName.equals(argList.get(i))) {
				tarList.add(argList.get(i+1));
			}
		}
		return tarList;
	}

	public String getOptionValue(String optName) {
		if(!argList.contains(optName)) {
			optName = getOptShortName(optName);
			if(!argList.contains(optName)) {
				return null;
			}
		}
		return argList.get(argList.lastIndexOf(optName) + 1);
	}

	public void addOpt(String optName, String shortOptName, String argDesc, String comments) {
		if("--help".equals(optName)) {
			log.warn("--help is allready had set !");
			return ;
		}
		if(optName != null && optName.length() > 0) {
			optList.add(new Opt(optName, shortOptName, argDesc, comments));
		}
	}

	/**
	 * 是否显示帮助：只有第一个参数为--help或者-h时才显示帮助
	 * @return
	 */
	public boolean isShowHelp() {
		if(argList.size() == 0) {
			return false;
		}
		return "--help".equals(argList.get(0)) || "-h".equals(argList.get(0));
	}

	public void showUsage() {
		System.out.println(" ======================================================= \n ");
		System.out.println(" usage : java -jar "+appName+" <options> \n");
		System.out.println(" ");

		for (String comments : commentsList) {
			System.out.println(" "+comments);
		}
		
		System.out.println(" options : ");
		
		for (Opt opt : optList) {
			StringBuilder sb = new StringBuilder();
			sb.append(" ")
				.append(opt.getOptName())
				.append(OPT_DELIMITER)
				.append(opt.getShortName())
				.append(OPT_DELIMITER)
				.append(opt.getArgDesc())
				.append(OPT_DELIMITER)
				.append(opt.getComments())
				.append(OPT_DELIMITER);
			
			System.out.println(sb.toString());
		}
		System.out.println("\n ======================================================= ");
	}
	
	@Data
	@AllArgsConstructor
	class Opt {
		private String optName;
		private String shortName;
		private String argDesc;
		private String comments;
	}

	public void addComments(String comments) {
		commentsList.add(comments);
	}
}

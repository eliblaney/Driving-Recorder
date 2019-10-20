package me.eli;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class DrivingRecorder {
	
	private final String log;
	private final HashMap<DrivingType, List<DrivingEntry>> records;
	
	public DrivingRecorder(String log) {
		this.log = log.replace("f", "").replace("r", "").replaceAll("//.*\n", "").replaceAll("(\\d{1,2})\\/(\\d+{1,2})\\/\\d{1,2}", "$1/$2");
		this.records = new HashMap<DrivingType, List<DrivingEntry>>();
		this.records.put(DrivingType.TOTAL, new ArrayList<DrivingEntry>());
		this.records.put(DrivingType.DAY, new ArrayList<DrivingEntry>());
		this.records.put(DrivingType.NIGHT, new ArrayList<DrivingEntry>());
		Calculator c = new Calculator();
		c.populate(this.log, records);
	}
	
	public String getLog() {
		return log;
	}
	
	private TimeResult time(long seconds) {
		long hours = 0, minutes = 0;
		if (seconds >= 3600)
			hours = (long) Math.floor(seconds / 3600);
		seconds -= 3600 * hours;
		if (seconds >= 60)
			minutes = (long) Math.floor(seconds / 60);
		seconds -= 60 * minutes;
		if (hours > 0)
			return new TimeResult(hours + " hour" + (hours != 1 ? "s" : "") + " " + minutes + " minute" + (minutes != 1 ? "s" : "") + " " + seconds + " seconds");
		return new TimeResult(minutes + " minute" + (minutes != 1 ? "s" : "") + " " + seconds + " second" + (seconds != 1 ? "s" : ""));
	}
	
	public TimeResult getTime(DrivingType type) {
		List<DrivingEntry> l = records.get(type);
		long seconds = 0;
		for(DrivingEntry e : l)
			seconds += e.seconds;
		return time(seconds);
	}
	
	public TimeResult averageDrivingTime(DrivingType type) {
		List<DrivingEntry> l = records.get(type);
		long seconds = 0;
		for(DrivingEntry e : l)
			seconds += e.seconds;
		if (l.size() == 0)
			return time(0);
		return time(seconds / l.size());
	}
	
	public TimeResult timePerDay(DrivingType type) {
		List<DrivingEntry> l = records.get(type);
		long seconds = 0;
		Set<String> dates = new HashSet<String>();
		for(DrivingEntry e : l)
			seconds += e.seconds;
		for(DrivingEntry e : l)
			dates.add(e.date);
		if (dates.size() == 0)
			return time(0);
		return time(seconds / dates.size());
	}
	
	public static void main(String... args) {
		if (args != null && args.length > 0 && args[0].equalsIgnoreCase("--nogui")) {
			try(BufferedReader r = new BufferedReader(new InputStreamReader(System.in))) {
				System.out.println("Format: MM/DD(/YY) <[f][n]<# ...>m<# ...>(s) ...>");
				System.out.println("Paste driving logs here:");
				String s = "";
				String line;
				while(!(line = r.readLine()).equals(""))
					s += "\n" + line;
				s = s.substring(1);
				DrivingRecorder d = new DrivingRecorder(s);
				System.out.println("--- RESULTS ---");
				System.out.println("Total driving time: " + d.getTime(DrivingType.TOTAL));
				System.out.println("Day driving time: " + d.getTime(DrivingType.DAY));
				System.out.println("Night driving time: " + d.getTime(DrivingType.NIGHT));
				System.out.println("Average driving time: " + d.averageDrivingTime(DrivingType.TOTAL));
				System.out.println("Estimated time per day: " + d.timePerDay(DrivingType.TOTAL));
			} catch(Throwable t) {
				System.err.println("There was a problem: " + t.getMessage());
				System.err.println("ABORT");
			}
		} else
			new Gui();
	}
	
	public static class Gui extends JFrame implements ActionListener {
		
		private static final long serialVersionUID = 1L;
		public static final String TOTAL_TEXT = "  Total Driving Time: ", DAY_TEXT = "|  Day Driving Time: ", NIGHT_TEXT = "  Night Driving Time: ", AVERAGE_TEXT = "|  Average Driving Time: ", ESTIMATED_TEXT = "  Estimated Time Per Day: ";
		private final JLabel total, day, night, average, estimated;
		private final JButton button;
		private final JTextArea logs;
		
		public Gui() {
			setName("Driving Recorder");
			setTitle("Driving Recorder");
			setMinimumSize(new Dimension(600, 550));
			setSize(600, 550);
			setLocationRelativeTo(null);
			setDefaultCloseOperation(EXIT_ON_CLOSE);
			setLayout(new FlowLayout());
			JPanel tp = new JPanel();
			JLabel title = new JLabel("Driving Recorder");
			title.setFont(new Font("Helvetica", Font.BOLD, 48));
			tp.add(title);
			add(tp);
			JPanel tbp = new JPanel();
			add(new JLabel());
			add(tbp);
			add(new JLabel());
			JPanel res = new JPanel();
			res.setLayout(new GridLayout(0, 2));
			res.add(total = new JLabel(TOTAL_TEXT + "Undefined"));
			res.add(day = new JLabel(DAY_TEXT + "Undefined"));
			res.add(night = new JLabel(NIGHT_TEXT + "Undefined"));
			res.add(average = new JLabel(AVERAGE_TEXT + "Undefined"));
			res.add(estimated = new JLabel(ESTIMATED_TEXT + "Undefined"));
			add(res);
			JPanel bottom = new JPanel();
			bottom.add(new JScrollPane(logs = new JTextArea("//Paste driving logs here! (Format: MM/DD(/YY) <[f][r][n][<#>h]<#>m<#>s ...>)", 20, 45)));
			add(bottom);
			JPanel copy = new JPanel();
			copy.setLayout(new GridLayout(0, 1));
			copy.add(button = new JButton("Calculate"));
			button.addActionListener(this);
			copy.add(new JLabel("Copyright © Eli Blaney 2016. All rights reserved."));
			add(copy);
			setVisible(true);
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				DrivingRecorder d = new DrivingRecorder(logs.getText());
				total.setText(TOTAL_TEXT + d.getTime(DrivingType.TOTAL).shorten());
				day.setText(DAY_TEXT + d.getTime(DrivingType.DAY).shorten());
				night.setText(NIGHT_TEXT + d.getTime(DrivingType.NIGHT).shorten());
				average.setText(AVERAGE_TEXT + d.averageDrivingTime(DrivingType.TOTAL).shorten());
				estimated.setText(ESTIMATED_TEXT + d.timePerDay(DrivingType.TOTAL).shorten());
			} catch(Throwable t) {
				JOptionPane.showMessageDialog(null, "Invalid logs!\nMake sure they match the format and try again.", "Invalid logs", JOptionPane.ERROR_MESSAGE);
			}
		}
	}
	
	public static class TimeResult {
		
		public final String text;
		
		public TimeResult(String text) {
			this.text = text;
		}
		
		public String shorten() {
			return this.text.replace("hour", "hr").replace("minute", "min").replace("second", "sec");
		}
		
		@Override
		public String toString() {
			return text;
		}
	}
	
	public static class Calculator {
		
		public void populate(String log, HashMap<DrivingType, List<DrivingEntry>> records) {
			for(String entry : log.split("\n")) {
				String[] elms = entry.split(" ");
				String date = elms[0];
				List<String> times = new ArrayList<String>();
				for(int i = 1; i < elms.length; i++)
					times.add(elms[i]);
				for(String time : times) {
					long seconds = 0;
					if (time.toLowerCase().contains("s"))
						seconds += Long.parseLong(time.split("m")[1].split("s")[0]);
					time = time.replace("s", "");
					boolean night = time.toLowerCase().contains("n");
					time = time.toLowerCase().replace("n", "");
					seconds = add(seconds, time);
					DrivingEntry e = new DrivingEntry(date, seconds);
					records.get(DrivingType.TOTAL).add(e);
					if (night)
						records.get(DrivingType.NIGHT).add(e);
					else
						records.get(DrivingType.DAY).add(e);
				}
			}
		}
		
		public long add(long seconds, String time) {
			if (time.contains("h")) {
				seconds += (Long.parseLong(time.split("m")[0].split("h")[1]) * 60);
				seconds += (Long.parseLong(time.split("m")[0].split("h")[0]) * 3600);
			} else
				seconds += (Long.parseLong(time.split("m")[0]) * 60);
			return seconds;
		}
	}
	
	public static class DrivingEntry {
		
		public final String date;
		public final long seconds;
		
		public DrivingEntry(String date, long seconds) {
			this.date = date;
			this.seconds = seconds;
		}
	}
	
	public static enum DrivingType {
		TOTAL, DAY, NIGHT;
	}
}
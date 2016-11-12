package com.app;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;


import com.google.gson.Gson;

/**
 * Hello world!
 *
 */
public class App {
	public static void main(String[] args) {

		JFrame jf = new JFrame();
		jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		jf.setSize(400, 300);
		jf.setVisible(true);

		// создаем панель.
		JPanel p = new JPanel();
		jf.add(p);
		final JTextField t1 = new JTextField("ak");
		final JTextField t2 = new JTextField("47");

		final JLabel l = new JLabel("result here ");

		// к панели добавляем менеджер FlowLayout.
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		JButton b = new JButton("okay");
		b.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				
				new Thread(	new Runnable(){
						public void run(){
							try {
								
								URL url = new URL("http://localhost:8080/usrs");
								HttpURLConnection connection = (HttpURLConnection) url.openConnection();
								connection.setConnectTimeout(5000);// 5 secs
								connection.setReadTimeout(5000);// 5 secs

								connection.setRequestMethod("POST");
								connection.setDoOutput(true);
								connection.setRequestProperty("Content-Type", "application/json");
								Login lg = new Login();
								lg.setLogin(t1.getText());
								lg.setPassword(t2.getText());

								OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
								out.write(new Gson().toJson(lg));
								System.out.println(new Gson().toJson(lg) + "!!!!!!!!!!!!!!!!!!!!");
								out.flush();
								out.close();

								int res = connection.getResponseCode();
								System.out.println(res);

								InputStream is = connection.getInputStream();
								BufferedReader br = new BufferedReader(new InputStreamReader(is));
								String line = null;
								while ((line = br.readLine()) != null) {
									System.out.println(line);
									l.setText(line);
								}
								connection.disconnect();
								while(true){}
							} catch (Throwable b) {
								b.printStackTrace();
							}
						}
					}).start();;
			};
		});

		// к панели добавляем кнопки.
		p.add(t1);
		p.add(t2);
		p.add(t2);
		p.add(b);
		p.add(l);
		jf.pack();

	}
}

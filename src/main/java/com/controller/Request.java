package com.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
import org.jpos.iso.packager.GenericPackager;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

@CrossOrigin
@RestController
@RequestMapping("/request")
public class Request {

	Logger logger = LoggerFactory.getLogger(Request.class);
	private String responseCode = null;
	private String responseDesc = null;

	private static SecretKeySpec secretKey;
	private static byte[] key;

	public static void setKey(String myKey) {
		MessageDigest sha = null;
		try {
			key = myKey.getBytes("UTF-8");
			sha = MessageDigest.getInstance("SHA-1");
			key = sha.digest(key);
			key = Arrays.copyOf(key, 16);
			secretKey = new SecretKeySpec(key, "AES");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	public static String encrypt(String strToEncrypt, String secret) {
		try {
			setKey(secret);
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE, secretKey);
			return Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.getBytes("UTF-8")));
		} catch (Exception e) {
			System.out.println("Error while encrypting: " + e.toString());
		}
		return null;
	}

	public static String decrypt(String strToDecrypt, String secret) {
		try {
			setKey(secret);
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
			cipher.init(Cipher.DECRYPT_MODE, secretKey);
			return new String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt)));
		} catch (Exception e) {
			System.out.println("Error while decrypting: " + e.toString());
		}
		return null;
	}

	@PostMapping(value = "/inquiry")
	public ResInqTirtanadi getInquiry(@RequestBody String body) {

		logger.info("[inquiry-wstirtanadi] Start Inquiry");

		ObjectMapper mapper = new ObjectMapper();

		ReqInqTirtanadiDataPelangganBit dataPelangganBit = new ReqInqTirtanadiDataPelangganBit();
		List<ResInqTirtanadiHasil> tirtanadiHasil = new ArrayList<ResInqTirtanadiHasil>();
		ResInqTirtanadiObject reqResIso = new ResInqTirtanadiObject();

		ResInqTirtanadi resTirtanadi = new ResInqTirtanadi();

		try {

			client.startConnection(url, port);

			dataPelangganBit = mapper.readValue(body, ReqInqTirtanadiDataPelangganBit.class);

			ISOMsg packageMsg = loadPackager();

			Boolean validateDataPelanggan = validateDataPelanggan(dataPelangganBit);

			if (validateDataPelanggan) {

				String bit41 = termid;

				logger.info("[inquiry-wstirtanadi] bit41 : " + bit41);

				packageMsg.set(0, "0100");
				packageMsg.set(7, new SimpleDateFormat("MMddHHmmss").format(new Date()));
				packageMsg.set(11, ISOUtil.zeropad(Long.toString(stan.incrementAndGet()), 6));
				packageMsg.set(33, dataPelangganBit.getIdPelanggan());
				packageMsg.set(39, "005");
				packageMsg.set(41, bit41);
				packageMsg.set(65, "");

				logger.info(
						"[inquiry-wstirtanadi] STAN MTI (SEMUA TUNGGAKAN PELANGGAN) : " + packageMsg.getString("11"));

				byte[] isoByteMsg = packageMsg.pack();

				String isoMessage = "";
				for (int i = 0; i < isoByteMsg.length; i++) {
					isoMessage += (char) isoByteMsg[i];
				}

				String header = String.format("%04d", isoMessage.length());

				String request = header + isoMessage;
				String unpackRequest = fromISOtoJSON(request.substring(4, request.length()));

				logger.info("[inquiry-wstirtanadi] Request to Biller MTI (SEMUA TUNGGAKAN PELANGGAN) : " + request);
				logger.info("[inquiry-wstirtanadi] Unpack Request to Biller MTI (SEMUA TUNGGAKAN PELANGGAN) : "
						+ unpackRequest);

				String resps = client.sendMessage(request);

				logger.info("[inquiry-wstirtanadi] Response Iso Biller MTI (SEMUA TUNGGAKAN PELANGGAN) : " + resps);

				String resp = fromISOtoJSON(resps.substring(4, resps.length()));

				logger.info("[inquiry-wstirtanadi] Convert Response to Json MTI (SEMUA TUNGGAKAN PELANGGAN) : " + resp);

				if (resp != null) {

					JSONParser parser = new JSONParser();
					JSONObject result = (JSONObject) parser.parse(resp);

					String errorcodedescs = "";

					if (result.get("57").equals("000")) {
						errorcodedescs = "Normal atau Tidak ada error";
					} else if (result.get("57").equals("001")) {
						errorcodedescs = "Terminal ID tidak terdaftar";
					} else if (result.get("57").equals("002")) {
						errorcodedescs = "Terminal ID tidak sesuai";
					} else if (result.get("57").equals("003")) {
						errorcodedescs = "IP tidak sesuai";
					} else if (result.get("57").equals("004")) {
						errorcodedescs = "Port tidak sesuai";
					} else if (result.get("57").equals("005")) {
						errorcodedescs = "IP tidak terdaftar";
					} else if (result.get("57").equals("006")) {
						errorcodedescs = "Stan masih aktif";
					} else if (result.get("57").equals("007")) {
						errorcodedescs = "Format ISO tidak sesuai";
					} else if (result.get("57").equals("008")) {
						errorcodedescs = "Paket data ISO tidak sesuai";
					}

					if (result.get("40").equals("000")) {

						reqResIso.setRescode(result.get("40").toString());
						reqResIso.setRescodedesc("Error");
						reqResIso.setReserrorcode(result.get("57").toString());
						reqResIso.setReserrorcodedesc(errorcodedescs);
						reqResIso.setRequestIsoBiller(header + isoMessage);
						reqResIso.setResponseIsoBillerTunggakan(resp);

					} else if (result.get("40").equals("001")) {

						String bit48 = result.get("48").toString();
						String lenghtbit48 = bit48.substring(3, bit48.length());

						String npa = lenghtbit48.substring(0, 2);
						String HasilNpa = lenghtbit48.substring(npa.length(), Integer.parseInt(npa) + npa.length());

						String nama = lenghtbit48.substring(npa.length() + HasilNpa.length(), lenghtbit48.length());
						String nonama = nama.substring(0, 2);
						String HasilNama = nama.substring(nonama.length(), Integer.parseInt(nonama) + nonama.length());

						String alamat = nama.substring(nonama.length() + HasilNama.length(), nama.length());
						String noalamat = alamat.substring(0, 2);
						String Hasilalamat = alamat.substring(noalamat.length(),
								Integer.parseInt(noalamat) + noalamat.length());

						String gol = alamat.substring(noalamat.length() + Hasilalamat.length(), alamat.length());
						String nogol = gol.substring(0, 2);
						String Hasilgol = gol.substring(nogol.length(), Integer.parseInt(nogol) + nogol.length());

						String tagihan = gol.substring(nogol.length() + Hasilgol.length(), gol.length());

						Integer arrayLength = tagihan.length();
						String detailLength = tagihan.substring(0, 2);
						Integer loop = arrayLength / (Integer.valueOf(detailLength) + detailLength.length());

						Integer counter2 = 0;

						List<HashMap<String, Object>> bit48array = new ArrayList<HashMap<String, Object>>();

						for (int i = 1; i <= loop; i++) {

							int beginIndex = counter2 + detailLength.length();
							int endIndex = counter2 + Integer.valueOf(detailLength) + detailLength.length();

							String detail = tagihan.substring(beginIndex, endIndex);
							counter2 = counter2 + Integer.valueOf(detailLength) + detailLength.length();

							HashMap<String, Object> map481 = new HashMap<String, Object>();
							map481.put("periode", detail.substring(0, 6));
							map481.put("standakhir", Integer.valueOf(detail.substring(6, 6 + 9)));
							map481.put("pemakaianM3", Integer.valueOf(detail.substring(6 + 9, 6 + 9 + 9)));
							map481.put("biaya", Integer.valueOf(detail.substring(6 + 9 + 9, 6 + 9 + 9 + 9)));
							map481.put("denda", Integer.valueOf(detail.substring(6 + 9 + 9 + 9, 6 + 9 + 9 + 9 + 9)));
							map481.put("adm",
									Integer.valueOf(detail.substring(6 + 9 + 9 + 9 + 9, 6 + 9 + 9 + 9 + 9 + 9)));
							map481.put("total", Integer
									.valueOf(detail.substring(6 + 9 + 9 + 9 + 9 + 9, 6 + 9 + 9 + 9 + 9 + 9 + 9)));

							bit48array.add(map481);
						}

						List<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();

						for (HashMap<String, Object> hashMap : bit48array) {

							HashMap<String, Object> res = new HashMap<String, Object>();
							res.put("npa", HasilNpa);
							res.put("nama", HasilNama);
							res.put("alamat", Hasilalamat);
							res.put("golonganTarif", Hasilgol);
							res.put("periode", hashMap.get("periode").toString());
							res.put("standAkhir", hashMap.get("standakhir").toString());
							res.put("pemakaianM3", hashMap.get("pemakaianM3").toString());
							res.put("tagihan", hashMap.get("biaya").toString());
							res.put("denda", hashMap.get("denda").toString());
							res.put("admin", hashMap.get("adm").toString());
							res.put("total", hashMap.get("total").toString());
							res.put("status", "");

							list.add(res);

						}

						String hasil = mapper.writeValueAsString(list);

						logger.info("[inquiry-wstirtanadi] SETELAH DI MAPPING ULANG : " + hasil);

						tirtanadiHasil = mapper.readValue(hasil, List.class);

						reqResIso.setRescode(result.get("40").toString());
						reqResIso.setRescodedesc("Data Ketemu");
						reqResIso.setReserrorcode(result.get("57").toString());
						reqResIso.setReserrorcodedesc(errorcodedescs);
						reqResIso.setRequestIsoBiller(header + isoMessage);
						reqResIso.setResponseIsoBillerTunggakan(resp);
						reqResIso.setResponseHasil(hasil);

						resTirtanadi.setDataiso(reqResIso);
						resTirtanadi.setResInqTirtanadiHasil(tirtanadiHasil);

						return resTirtanadi;

					} else if (result.get("40").equals("002")) {

						reqResIso.setRescode(result.get("40").toString());
						reqResIso.setRescodedesc("ID Tidak Terdaftar");
						reqResIso.setReserrorcode(result.get("57").toString());
						reqResIso.setReserrorcodedesc(errorcodedescs);
						reqResIso.setRequestIsoBiller(header + isoMessage);
						reqResIso.setResponseIsoBillerTunggakan(resp);

					} else if (result.get("40").equals("003")) {

						reqResIso.setRescode(result.get("40").toString());
						reqResIso.setRescodedesc("ID Sudah Putus");
						reqResIso.setReserrorcode(result.get("57").toString());
						reqResIso.setReserrorcodedesc(errorcodedescs);
						reqResIso.setRequestIsoBiller(header + isoMessage);
						reqResIso.setResponseIsoBillerTunggakan(resp);

					}

				} else {

					reqResIso.setRescode("99");
					reqResIso.setRescodedesc("Other Error");
					reqResIso.setRequestIsoBiller(header + isoMessage);
					reqResIso.setResponseIsoBillerTunggakan("");
					reqResIso.setResponseIsoBillerTagihan("");
					reqResIso.setResponseHasil("");

					resTirtanadi.setDataiso(reqResIso);
					resTirtanadi.setResInqTirtanadiHasil(tirtanadiHasil);

					return resTirtanadi;
				}

			} else {

				reqResIso.setRescode(responseCode);
				reqResIso.setRescodedesc(responseDesc);

			}

			resTirtanadi.setDataiso(reqResIso);
			resTirtanadi.setResInqTirtanadiHasil(tirtanadiHasil);

			return resTirtanadi;

		} catch (Exception e) {

			e.printStackTrace();
			reqResIso.setRescode("99");
			reqResIso.setRescodedesc("Other Error");
			resTirtanadi.setDataiso(reqResIso);
			resTirtanadi.setResInqTirtanadiHasil(tirtanadiHasil);

			return resTirtanadi;

		}

	}

	@PostMapping(value = "/payment")
	public ResPayTirtanadi getPayment(@RequestBody String body) {

		logger.info("[payment-wstirtanadi] Start Payment");

		ObjectMapper mapper = new ObjectMapper();

		List<ReqPayTirtanadiBit> reqTnTagihan = new ArrayList<ReqPayTirtanadiBit>();
		ResPayTirtanadiBit resBit = new ResPayTirtanadiBit();
		ResPayTirtanadiObject reqResIso = new ResPayTirtanadiObject();

		ResPayTirtanadi resTirtanadi = new ResPayTirtanadi();

		try {

			client.startConnection(url, port);

			reqTnTagihan = mapper.readValue(body, List.class);

			String a = mapper.writeValueAsString(reqTnTagihan);

			List<HashMap<String, Object>> listrs = mapper.readValue(a, List.class);

			Integer jumLL = 0;
			String dataLLL = "";
			Integer j = 0;

			for (HashMap<String, Object> res : listrs) {

				String b = mapper.writeValueAsString(res);
				ReqPayTirtanadiBit req = mapper.readValue(b, ReqPayTirtanadiBit.class);

				Boolean validate = validatePay(req);

				if (!validate) {

					reqResIso.setRescode(responseCode);
					reqResIso.setRescodedesc(responseDesc);

				}

				String tahun = req.getPeriode().substring(0, 4);
				String bulan = req.getPeriode().substring(4, 6);

				String jumLLs = bulan + tahun;

				Integer jumlenght = jumLLs.length();

				if (jumlenght.toString().length() == 1) {
					dataLLL += "0" + jumLLs.length() + jumLLs;
				} else {
					dataLLL += jumLLs.length() + jumLLs;
				}

				jumLL = dataLLL.length();

				j++;
			}

			String LLL = "";

			if (jumLL.toString().length() == 1) {
				LLL = "00" + jumLL;
			} else if (jumLL.toString().length() == 2) {
				LLL = "0" + jumLL;
			} else if (jumLL.toString().length() == 3) {
				LLL = jumLL.toString();
			}

			logger.info("[payment-wstirtanadi] Bit47 " + dataLLL);

			Integer jumlah = j;

			String bit25 = "";
			if (jumlah.toString().length() == 1) {

				bit25 = "000" + jumlah;
			} else if (jumlah.toString().length() == 2) {

				bit25 = "00" + jumlah;
			} else if (jumlah.toString().length() == 3) {

				bit25 = "0" + jumlah;
			} else if (jumlah.toString().length() == 3) {

				bit25 = jumlah.toString();
			}

			logger.info("[payment-wstirtanadi] Bit25 " + bit25);

			ISOMsg packageMsg = loadPackager();

			String bit41 = termid;

			packageMsg.set(0, "0200");
			packageMsg.set(7, new SimpleDateFormat("MMddHHmmss").format(new Date()));
			packageMsg.set(11, ISOUtil.zeropad(Long.toString(stan.incrementAndGet()), 6));
			packageMsg.set(25, bit25);
			packageMsg.set(32, listrs.get(0).get("acquirer").toString());
			packageMsg.set(33, listrs.get(0).get("idPelanggan").toString());
			packageMsg.set(39, "002");
			packageMsg.set(41, bit41);
			packageMsg.set(47, dataLLL);
			packageMsg.set(65, "");

			byte[] isoByteMsg = packageMsg.pack();

			String isoMessage = "";
			for (int i = 0; i < isoByteMsg.length; i++) {
				isoMessage += (char) isoByteMsg[i];
			}

//			logger.info("[request-wstirtanadi] Req Debug : " + isoMessage);
			String header = String.format("%04d", isoMessage.length());

			String resp1 = client.sendMessage(header + isoMessage);

			String resp3 = fromISOtoJSON(resp1.substring(4, resp1.length()));

			logger.info("[payment-wstirtanadi] Request Iso to Biller (MULTI PEMBAYARAN) : " + header + isoMessage);

			String request2 = header + isoMessage;
			String unpackRequest2 = fromISOtoJSON(request2.substring(4, request2.length()));
			logger.info("[inquiry-wstirtanadi] Unpack Request to Biller : " + unpackRequest2);

			logger.info("[payment-wstirtanadi] Response Payment Iso Biller (MULTI PEMBAYARAN) : " + resp1);

			logger.info("[payment-wstirtanadi] Convert Response to Json (MULTI PEMBAYARAN) : " + resp3);

			if (resp1 != null) {

				JSONParser parser = new JSONParser();
				JSONObject result = (JSONObject) parser.parse(resp3);

				// MAPPING BIT 48
				String bit481 = result.get("48").toString();

				String arrayLength = bit481.substring(0, 3);
				String detailLength = bit481.substring(3, 5);
				Integer loop = Integer.valueOf(arrayLength) / (Integer.valueOf(detailLength) + detailLength.length());

				Integer counter1 = arrayLength.toString().length();
				Integer counter2 = 0;

				List<HashMap<String, Object>> bit48array = new ArrayList<HashMap<String, Object>>();

				for (int i = 1; i <= loop; i++) {

					int beginIndex = counter1 + counter2 + detailLength.length();
					int endIndex = counter1 + counter2 + Integer.valueOf(detailLength) + detailLength.length();

					String detail = bit481.substring(beginIndex, endIndex);
					counter2 = counter2 + Integer.valueOf(detailLength) + detailLength.length();

					HashMap<String, Object> map481 = new HashMap<String, Object>();
					map481.put("bulan", detail.substring(0, 2));
					map481.put("tahun", detail.substring(2, 6));
					map481.put("responseCode", detail.substring(6, 9));

					bit48array.add(map481);

				}
				// MAPPING BIT 48

				HashMap<String, Object> rs = new HashMap<String, Object>();
				rs.put("bitMTI", result.get("MTI").toString());
				rs.put("bit7", result.get("7").toString());
				rs.put("bit11", result.get("11").toString());
				rs.put("bit12", result.get("12").toString());
				rs.put("bit25", result.get("25").toString());
				rs.put("bit32", result.get("32").toString());
				rs.put("bit33", result.get("33").toString());
				rs.put("bit39", result.get("39").toString());
				rs.put("bit40", result.get("40").toString());
				rs.put("bit41", result.get("41").toString());
				rs.put("bit47", result.get("47").toString());
				rs.put("bit48", bit48array);
				rs.put("bit57", result.get("57").toString());

				String rss = mapper.writeValueAsString(rs);

				resBit = mapper.readValue(rss, ResPayTirtanadiBit.class);

				String errorcodedesc = "";

				if (resBit.getBit57().equals("000")) {
					errorcodedesc = "Normal atau Tidak ada error";
				} else if (resBit.getBit57().equals("001")) {
					errorcodedesc = "Terminal ID tidak terdaftar";
				} else if (resBit.getBit57().equals("002")) {
					errorcodedesc = "Terminal ID tidak sesuai";
				} else if (resBit.getBit57().equals("003")) {
					errorcodedesc = "IP tidak sesuai";
				} else if (resBit.getBit57().equals("004")) {
					errorcodedesc = "Port tidak sesuai";
				} else if (resBit.getBit57().equals("005")) {
					errorcodedesc = "IP tidak terdaftar";
				} else if (resBit.getBit57().equals("006")) {
					errorcodedesc = "Stan masih aktif";
				} else if (resBit.getBit57().equals("007")) {
					errorcodedesc = "Format ISO tidak sesuai";
				} else if (resBit.getBit57().equals("008")) {
					errorcodedesc = "Paket data ISO tidak sesuai";
				}

				if (resBit.getBit40().equals("001")) {

					reqResIso.setRescode(resBit.getBit40());
					reqResIso.setRescodedesc("Berhasil Bayar Semua");
					reqResIso.setReserrorcode(resBit.getBit57());
					reqResIso.setReserrorcodedesc(errorcodedesc);
					reqResIso.setRequestIsoBiller(header + isoMessage);
					reqResIso.setResponseIsoBiller(resp1);

				} else if (resBit.getBit40().equals("002")) {

					reqResIso.setRescode(resBit.getBit40());
					reqResIso.setRescodedesc("Gagal bayar (informasi di  respon bit 48)");
					reqResIso.setReserrorcode(resBit.getBit57());
					reqResIso.setReserrorcodedesc(errorcodedesc);
					reqResIso.setRequestIsoBiller(header + isoMessage);
					reqResIso.setResponseIsoBiller(resp1);

				} else if (resBit.getBit40().equals("003")) {

					reqResIso.setRescode(resBit.getBit40());
					reqResIso.setRescodedesc("Error");
					reqResIso.setReserrorcode(resBit.getBit57());
					reqResIso.setReserrorcodedesc(errorcodedesc);
					reqResIso.setRequestIsoBiller(header + isoMessage);
					reqResIso.setResponseIsoBiller(resp1);

				} else if (resBit.getBit40().equals("004")) {

					reqResIso.setRescode(resBit.getBit40());
					reqResIso.setRescodedesc("Data waktu tagihan tidak valid");
					reqResIso.setReserrorcode(resBit.getBit57());
					reqResIso.setReserrorcodedesc(errorcodedesc);
					reqResIso.setRequestIsoBiller(header + isoMessage);
					reqResIso.setResponseIsoBiller(resp1);

				} else if (resBit.getBit40().equals("005")) {

					reqResIso.setRescode(resBit.getBit40());
					reqResIso.setRescodedesc("Sudah di putus");
					reqResIso.setReserrorcode(resBit.getBit57());
					reqResIso.setReserrorcodedesc(errorcodedesc);
					reqResIso.setRequestIsoBiller(header + isoMessage);
					reqResIso.setResponseIsoBiller(resp1);
				}

			} else {

				reqResIso.setRescode("99");
				reqResIso.setRescodedesc("Other Error");
				reqResIso.setRequestIsoBiller(header + isoMessage);
				reqResIso.setResponseIsoBiller(resp1);
			}

			resTirtanadi.setDataiso(reqResIso);
			resTirtanadi.setResBit(resBit);

			return resTirtanadi;

		} catch (Exception e) {

			reqResIso.setRescode("99");
			reqResIso.setRescodedesc("Other Error");
			resTirtanadi.setDataiso(reqResIso);
			resTirtanadi.setResBit(resBit);

			return resTirtanadi;

		}

	}

}

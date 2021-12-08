package com.controller;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.model.ReqEncDec;
import com.model.ResDec;
import com.model.ResDecJson;
import com.model.ResDecObject;
import com.model.ResEnc;
import com.model.ResEncJson;
import com.model.ResEncObject;

@CrossOrigin
@RestController
@RequestMapping("/request")
public class Request {

	@Value("${ws.secretKeys}")
	private String secretKeys;

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

	public boolean validate(ReqEncDec reqTnBit) {

		try {

			if (reqTnBit.getInput().isEmpty()) {
				responseCode = "90";
				responseDesc = "Inputan kosong";
				return false;
			}

			return true;

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

	}

	@PostMapping(value = "/reqEncrypt")
	public ResEnc getEncrypt(@RequestBody String body) {

		logger.info("[ws-enc-dec] Start Request Encrypt");

		ReqEncDec reqEncDec = new ReqEncDec();

		ResEnc resEncDec = new ResEnc();
		ResEncObject resEncDecObject = new ResEncObject();
		ResEncJson resEncDecJson = new ResEncJson();

		try {
			ObjectMapper mapper = new ObjectMapper();

			reqEncDec = mapper.readValue(body, ReqEncDec.class);

			Boolean validate = validate(reqEncDec);

			if (validate) {

				String encryptedString = encrypt(reqEncDec.getInput(), secretKeys);

				resEncDecObject.setRescode("00");
				resEncDecObject.setRescodedesc("Sukses");

				resEncDecJson.setInput(reqEncDec.getInput());
				resEncDecJson.setEncrypted(encryptedString);

			} else {

				resEncDecObject.setRescode(responseCode);
				resEncDecObject.setRescodedesc(responseDesc);

			}

			resEncDec.setResEncObject(resEncDecObject);
			resEncDec.setResEncJson(resEncDecJson);

			return resEncDec;

		} catch (Exception e) {

			resEncDecObject.setRescode("99");
			resEncDecObject.setRescodedesc("Other Error");
			resEncDec.setResEncObject(resEncDecObject);
			resEncDec.setResEncJson(resEncDecJson);

			return resEncDec;

		}
	}

	@PostMapping(value = "/reqDecrypt")
	public ResDec getDecrypt(@RequestBody String body) {

		logger.info("[ws-enc-dec] Start Request Decrypt");

		ReqEncDec reqEncDec = new ReqEncDec();

		ResDec resEncDec = new ResDec();
		ResDecObject resEncDecObject = new ResDecObject();
		ResDecJson resEncDecJson = new ResDecJson();

		try {
			ObjectMapper mapper = new ObjectMapper();

			reqEncDec = mapper.readValue(body, ReqEncDec.class);

			Boolean validate = validate(reqEncDec);

			if (validate) {

				String decryptedString = decrypt(reqEncDec.getInput(), secretKeys);

				resEncDecObject.setRescode("00");
				resEncDecObject.setRescodedesc("Sukses");

				resEncDecJson.setInput(reqEncDec.getInput());
				resEncDecJson.setEncrypted(decryptedString);

			} else {

				resEncDecObject.setRescode(responseCode);
				resEncDecObject.setRescodedesc(responseDesc);

			}

			resEncDec.setResDecObject(resEncDecObject);
			resEncDec.setResDecJson(resEncDecJson);

			return resEncDec;

		} catch (Exception e) {

			resEncDecObject.setRescode("99");
			resEncDecObject.setRescodedesc("Other Error");
			resEncDec.setResDecObject(resEncDecObject);
			resEncDec.setResDecJson(resEncDecJson);

			return resEncDec;

		}

	}

}

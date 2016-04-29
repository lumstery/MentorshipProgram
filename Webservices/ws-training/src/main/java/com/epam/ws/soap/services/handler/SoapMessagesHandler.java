package com.epam.ws.soap.services.handler;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import org.w3c.dom.Element;

import com.epam.ws.service.CurrencyService;
import com.epam.ws.service.holder.ServiceFactory;
import com.epam.ws.util.CurrencyConversionUtil;

public class SoapMessagesHandler implements SOAPHandler<SOAPMessageContext> {

	private CurrencyService currencyService = ServiceFactory.getInstance().getCurrencyService();

	@Override
	public boolean handleMessage(SOAPMessageContext context) {

		System.out.println("Server : handleMessage()......");

		Boolean isRequest = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

		// for response message only, true for outbound messages, false for
		// inbound

		try {
			SOAPMessage soapMsg = context.getMessage();
			SOAPEnvelope soapEnv = soapMsg.getSOAPPart().getEnvelope();
			SOAPHeader soapHeader = soapEnv.getHeader();
			SOAPElement parentElement = soapMsg.getSOAPBody().getParentElement();
			Element ammountElement = (Element) parentElement.getChildElements(new QName("ammount")).next();
			Element currencyElement = (Element) parentElement.getChildElements(new QName("currency")).next();
			Element userIdElement = (Element) parentElement.getChildElements(new QName("userId")).next();

			if (!isRequest) {
				Element needConvertElement =(Element) soapHeader.getChildElements(new QName("CC")).next();
				if (Boolean.valueOf(needConvertElement.getTextContent())) {
					Double previousAmount = Double.parseDouble(ammountElement.getTextContent());
					Double convertedAmmount = CurrencyConversionUtil.convertToDefault(
							currencyService.getByAlias(currencyElement.getTextContent()), previousAmount);
					ammountElement.setTextContent(String.valueOf(convertedAmmount));
				}
			} else {
				String stringToHash = ammountElement.getTextContent()+currencyElement.getTextContent()+userIdElement.getTextContent();
				byte[] digest = MessageDigest.getInstance("MD5").digest(stringToHash.getBytes(Charset.forName("UTF-8")));
				soapHeader.addChildElement("hash").setTextContent(new String(digest));
			}

		} catch (SOAPException | NoSuchAlgorithmException e) {
			System.err.println(e);
		}

		return true;
	}

	@Override
	public boolean handleFault(SOAPMessageContext context) {
		System.out.println("Server : handleFault()......");
		return true;
	}

	@Override
	public void close(MessageContext context) {
		System.out.println("Server : close()......");
	}

	@Override
	public Set<QName> getHeaders() {
		System.out.println("Server : getHeaders()......");
		return null;
	}

}
package com.epam.spring.dao.impl;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Repository;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.epam.spring.dao.CurrencyDAO;
import com.epam.spring.dao.TransactionDAO;
import com.epam.spring.model.Transaction;
import com.epam.spring.model.Transaction.OperationType;
import com.epam.spring.util.DOMProcessor;
import com.epam.spring.util.XmlToJavaDomMapper;
import com.epam.spring.util.XmlToJavaDomMapper.XmlToJavaMapper;


@Repository
public class TransactionDAOImpl implements TransactionDAO {

	private DocumentBuilderFactory dbFactory;
	private DocumentBuilder dBuilder;
	private File transactionsXmlStorageFile;
	private Document transactionsXmlStorageDocument;
	private XmlToJavaDomMapper<Transaction> xmlToJavaDomMapper;
	@Autowired
	private CurrencyDAO currencyDao;
	@Autowired
	private Environment environment;
	
	@PostConstruct
	private void init() throws ParserConfigurationException, SAXException, IOException,
			TransformerFactoryConfigurationError, TransformerException {
		dbFactory = DocumentBuilderFactory.newInstance();
		dBuilder = dbFactory.newDocumentBuilder();
		transactionsXmlStorageFile = new File(environment.getProperty("transactions.xml.file.name"));
		if (transactionsXmlStorageFile.exists()) {
			updateDocumentFromFile();
		} else {
			transactionsXmlStorageFile.createNewFile();
			transactionsXmlStorageDocument = dBuilder.newDocument();
			Element transactionsElement = transactionsXmlStorageDocument.createElement("transactions");
			transactionsXmlStorageDocument.appendChild(transactionsElement);
			saveDocumentToFile(transactionsXmlStorageDocument);
		}
		
		xmlToJavaDomMapper = new XmlToJavaDomMapper<>(new XmlToJavaMapper<Transaction>() {

			@Override
			public Transaction convert(Element element) {
				Transaction transaction = new Transaction();
				transaction.setAmmount(Double.valueOf(element.getElementsByTagName("ammount").item(0).getNodeValue()));
				transaction.setAmountInDefaultCurrency(Double.valueOf(element.getElementsByTagName("ammountInDefaultCurrency").item(0).getNodeValue()));
				transaction.setConvertedToDefaultCurrency(Boolean.valueOf(element.getElementsByTagName("isConverted").item(0).getNodeValue()));
				transaction.setCurrency(currencyDao.getByAlias(element.getElementsByTagName("currency").item(0).getNodeValue()));
				transaction.setId(Long.valueOf(element.getAttribute("id")));
				transaction.setUserId(Long.valueOf(element.getAttribute("userId")));
				transaction.setOperationType(OperationType.valueOf(element.getAttribute("operation")));
				return transaction;
			}
		});
	}

	private void updateDocumentFromFile() {
		try {
			transactionsXmlStorageDocument = dBuilder.parse(transactionsXmlStorageFile);
		} catch (SAXException | IOException e) {
			e.printStackTrace();
		}
	}
	
	private void commitData(){
		try {
			saveDocumentToFile(transactionsXmlStorageDocument);
		} catch (TransformerException e) {
			e.printStackTrace();
		}
	}
	
	private void saveDocumentToFile(Document document) throws TransformerException  {
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		Result output = new StreamResult(transactionsXmlStorageFile);
		Source input = new DOMSource(document);
		transformer.transform(input, output);
	}

	@Override
	public Transaction add(Transaction transaction) {
		updateDocumentFromFile();
		Element root = transactionsXmlStorageDocument.getDocumentElement();
		
		int itemsInFile = root.getElementsByTagName("transaction").getLength();
		transaction.setId((long) ++itemsInFile);
		
		Element newTransactionNode = new DOMProcessor(transactionsXmlStorageDocument)
		.newElement("transaction")
		.addAttribute("id", transaction.getId())
		.addAttribute("userId", transaction.getUserId())
		.addAttribute("operation", transaction.getOperationType().name())
		.addChild("currency")
			.setValue(transaction.getCurrency().getAlias())
			.insertInto()
			.outerElement()
		.addChild("isConverted")
			.setValue(transaction.isConvertedToDefaultCurrency())
			.insertInto()
			.outerElement()
		.addChild("ammount")	
			.setValue(transaction.getAmmount())
			.insertInto()
			.outerElement()
		.addChild("ammountInDefaultCurrency")
			.setValue(transaction.getAmountInDefaultCurrency())
			.insertInto()
			.outerElement()
		.build();	

		root.appendChild(newTransactionNode);
		commitData();
		return transaction;
	}

	@Override
	public void update(Transaction transaction) {
		Element root = transactionsXmlStorageDocument.getDocumentElement();
				
		Element newTransactionNode = new DOMProcessor(transactionsXmlStorageDocument)
		.newElement("transaction")
		.addAttribute("id", transaction.getId())
		.addAttribute("userId", transaction.getUserId())
		.addAttribute("operation", transaction.getOperationType().name())
		.addChild("currency")
			.setValue(transaction.getCurrency().getAlias())
			.insertInto()
			.outerElement()
		.addChild("isConverted")
			.setValue(transaction.isConvertedToDefaultCurrency())
			.insertInto()
			.outerElement()
		.addChild("ammount")	
			.setValue(transaction.getAmmount())
			.insertInto()
			.outerElement()
		.addChild("ammountInDefaultCurrency")
			.setValue(transaction.getAmountInDefaultCurrency())
			.insertInto()
			.outerElement()
		.build();	

		
		Element nodeOfCurrentTransaction = new DOMProcessor(transactionsXmlStorageDocument)
		.findChildByCriteria(root)
		.defineCriterias()
		.attributeIsEqualTo("id", transaction.getId())
		.enought()
		.getSingleResult();
	
		root.replaceChild(newTransactionNode,nodeOfCurrentTransaction);
		commitData();
		
	}

	@Override
	public void remove(Long key) {
		Element root = transactionsXmlStorageDocument.getDocumentElement();
		
		Element nodeOfCurrentTransaction = new DOMProcessor(transactionsXmlStorageDocument)
				.findChildByCriteria(root)
				.defineCriterias()
				.attributeIsEqualTo("id", key)
				.enought()
				.getSingleResult();
		
		root.removeChild(nodeOfCurrentTransaction);
		commitData();
		
	}

	@Override
	public List<Transaction> getAll() {
		Element root = transactionsXmlStorageDocument.getDocumentElement();
		return xmlToJavaDomMapper.mapXmlElementsToListOfClasses(root.getChildNodes()) ;
	}

	@Override
	public Transaction getById(Long key) {
		return xmlToJavaDomMapper.mapXmlElementToObject(new DOMProcessor(transactionsXmlStorageDocument)
		.findChildByCriteria(transactionsXmlStorageDocument.getDocumentElement())
		.defineCriterias()
		.attributeIsEqualTo("id", key)
		.enought()
		.getSingleResult());
		
		
	}

	@Override
	public List<Transaction> getAllForUser(Long userId) {
		return xmlToJavaDomMapper.mapXmlElementsToListOfClasses(new DOMProcessor(transactionsXmlStorageDocument)
				.findChildByCriteria(transactionsXmlStorageDocument.getDocumentElement())
				.defineCriterias()
				.attributeIsEqualTo("userId", userId)
				.enought()
				.getResultList());
	}

}

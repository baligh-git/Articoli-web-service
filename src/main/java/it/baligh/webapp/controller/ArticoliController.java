package it.baligh.webapp.controller;

import java.util.List;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponses;
import it.baligh.webapp.entities.Articoli;
import it.baligh.webapp.entities.Barcode;
import it.baligh.webapp.exception.BindingException;
import it.baligh.webapp.exception.DuplicateException;
import it.baligh.webapp.exception.NotFoundException;
import it.baligh.webapp.service.ArticoliService;
import it.baligh.webapp.service.BarcodeService;
import io.swagger.annotations.ApiResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@RestController
@RequestMapping("/articoli")
public class ArticoliController
{
	private static final Logger logger = LoggerFactory.getLogger(ArticoliController.class);

	@Autowired
	private BarcodeService barcodeService;

	@Autowired
	private ArticoliService articoliService;

	@Autowired
	private ResourceBundleMessageSource errMessage;
	
	@ApiOperation(
		      value = "Ricerca l'articolo per codice a barre", 
		      notes = "Restituisce i dati dell'articolo in formato JSON",
		      response = Articoli.class, 
		      produces = "application/json")
	@ApiResponses(value =
	{ @ApiResponse(code = 200, message = "Articolo Trovato"),
	  @ApiResponse(code = 404, message = "Articolo Non Trovato")})
	@RequestMapping(value = "/cerca/ean/{barcode}", method = RequestMethod.GET, produces = "application/json")
	public ResponseEntity<Articoli> listArtByEan(@ApiParam("Barcode univoco dell'articolo") @PathVariable("barcode") String Barcode)
		throws NotFoundException	 
	{
		logger.info("****** Otteniamo l'articolo con barcode " + Barcode + " *******");

		Articoli articolo;
		Barcode Ean = barcodeService.SelByBarcode(Barcode);
		
		if (Ean == null)
		{
			String ErrMsg = String.format("Il barcode %s non è stato trovato!", Barcode);
			
			logger.warn(ErrMsg);
			
			throw new NotFoundException(ErrMsg);
			//return new ResponseEntity<Articoli>(HttpStatus.NOT_FOUND);
		}
		else
			articolo = Ean.getArticolo();

		return new ResponseEntity<Articoli>(articolo, HttpStatus.OK);
	}
	
	// ------------------- Ricerca Per Codice ------------------------------------
	@RequestMapping(value = "/cerca/codice/{codart}", method = RequestMethod.GET, produces = "application/json")
	public ResponseEntity<Articoli> listArtByCodArt(@PathVariable("codart") String CodArt)  
			throws NotFoundException
	{
		logger.info("****** Otteniamo l'articolo con codice " + CodArt + " *******");

		Articoli articolo = articoliService.SelByCodArt(CodArt);

		if (articolo == null)
		{
			String ErrMsg = String.format("L'articolo con codice %s non è stato trovato!", CodArt);
			
			logger.warn(ErrMsg);
			
			throw new NotFoundException(ErrMsg);
		}

		return new ResponseEntity<Articoli>(articolo, HttpStatus.OK);
	}

	// ------------------- Ricerca Per Descrizione ------------------------------------
	@RequestMapping(value = "/cerca/descrizione/{filter}", method = RequestMethod.GET, produces = "application/json")
	public ResponseEntity<List<Articoli>> listArtByDesc(@PathVariable("filter") String Filter)
			throws NotFoundException
	{
		logger.info("****** Otteniamo gli articoli con Descrizione: " + Filter + " *******");

		List<Articoli> articoli = articoliService.SelByDescrizione(Filter + "%");

		if (articoli == null)
		{
			String ErrMsg = String.format("Non è stato trovato alcun articolo avente descrizione %s", Filter);
			
			logger.warn(ErrMsg);
			
			throw new NotFoundException(ErrMsg);
			
		}

		return new ResponseEntity<List<Articoli>>(articoli, HttpStatus.OK);
	}

	// ------------------- Ricerca Per Descrizione Con Paging------------------------------------
	@RequestMapping(value = "/cerca/descrizione/{filter}/{page}/{rows}", method = RequestMethod.GET, 
			produces = "application/json")
	public ResponseEntity<List<Articoli>> listArtByDescPag(@PathVariable("filter") String Filter,
			@PathVariable("page") int PageNum, @PathVariable("rows") int RecXPage) 
					throws NotFoundException
	{
		logger.info("****** Otteniamo gli articoli con Descrizione: " + Filter + " *******");

		RecXPage = (RecXPage < 0) ? 10 : RecXPage;

		List<Articoli> articoli = articoliService.SelByDescrizione(Filter + "%", PageRequest.of(PageNum, RecXPage));

		if (articoli == null)
		{
			String ErrMsg = String.format("Non è stato trovato alcun articolo avente il parametro %s", Filter);
			
			logger.warn(ErrMsg);
			
			throw new NotFoundException(ErrMsg);
			
		}

		return new ResponseEntity<List<Articoli>>(articoli, HttpStatus.OK);
	}

	// ------------------- INSERIMENTO ARTICOLO ------------------------------------
	@RequestMapping(value = "/inserisci", method = RequestMethod.POST)
	public ResponseEntity<Articoli> createArt(@Valid @RequestBody Articoli articolo, BindingResult bindingResult,
			UriComponentsBuilder ucBuilder) 
			throws BindingException, DuplicateException
	{
		logger.info("Salviamo l'articolo con codice " + articolo.getCodArt());
		
		if (bindingResult.hasErrors())
		{
			String MsgErr = errMessage.getMessage(bindingResult.getFieldError(), LocaleContextHolder.getLocale());
			
			logger.warn(MsgErr);

			throw new BindingException(MsgErr);
		}
		 
		//Disabilitare se si vuole gestire anche la modifica 
		Articoli checkArt =  articoliService.SelByCodArt(articolo.getCodArt());

		if (checkArt != null)
		{
			String MsgErr = String.format("Articolo %s presente in anagrafica! "
					+ "Impossibile utilizzare il metodo POST", articolo.getCodArt());
			
			logger.warn(MsgErr);
			
			throw new DuplicateException(MsgErr);
		}
		
		HttpHeaders headers = new HttpHeaders();
		ObjectMapper mapper = new ObjectMapper();
		
		headers.setContentType(MediaType.APPLICATION_JSON);

		ObjectNode responseNode = mapper.createObjectNode();

		articoliService.InsArticolo(articolo);
		
		responseNode.put("code", HttpStatus.OK.toString());
		responseNode.put("message", "Inserimento Articolo " + articolo.getCodArt() + " Eseguita Con Successo");

		return new ResponseEntity<Articoli>(headers, HttpStatus.CREATED);
	}

	// ------------------- MODIFICA ARTICOLO ------------------------------------
	@RequestMapping(value = "/modifica", method = RequestMethod.PUT)
	public ResponseEntity<Articoli> updateArt(@Valid @RequestBody Articoli articolo, BindingResult bindingResult,
				UriComponentsBuilder ucBuilder) throws BindingException,NotFoundException  
	{
		logger.info("Modifichiamo l'articolo con codice " + articolo.getCodArt());
		
		if (bindingResult.hasErrors())
		{
			String MsgErr = errMessage.getMessage(bindingResult.getFieldError(), LocaleContextHolder.getLocale());
			
			logger.warn(MsgErr);

			throw new BindingException(MsgErr);
		}
		
		Articoli checkArt =  articoliService.SelByCodArt(articolo.getCodArt());

		if (checkArt == null)
		{
			String MsgErr = String.format("Articolo %s non presente in anagrafica! "
					+ "Impossibile utilizzare il metodo PUT", articolo.getCodArt());
			
			logger.warn(MsgErr);
			
			throw new NotFoundException(MsgErr);
		}
		
		HttpHeaders headers = new HttpHeaders();
		ObjectMapper mapper = new ObjectMapper();
		
		headers.setContentType(MediaType.APPLICATION_JSON);

		ObjectNode responseNode = mapper.createObjectNode();

		articoliService.InsArticolo(articolo);
		
		responseNode.put("code", HttpStatus.OK.toString());
		responseNode.put("message", "Modifica Articolo " + articolo.getCodArt() + " Eseguita Con Successo");

		return new ResponseEntity<Articoli>(headers, HttpStatus.CREATED);
	}
    
   // ------------------- ELIMINAZIONE ARTICOLO ------------------------------------
	@RequestMapping(value = "/elimina/{codart}", method = RequestMethod.DELETE)
	public ResponseEntity<?> deleteArt(@PathVariable("codart") String CodArt)
			throws  NotFoundException 
	{
		logger.info("Eliminiamo l'articolo con codice " + CodArt);

		HttpHeaders headers = new HttpHeaders();
		ObjectMapper mapper = new ObjectMapper();

		headers.setContentType(MediaType.APPLICATION_JSON);

		ObjectNode responseNode = mapper.createObjectNode();

		Articoli articolo = articoliService.SelByCodArt(CodArt);

		if (articolo == null)
		{
			String MsgErr = String.format("Articolo %s non presente in anagrafica! ",CodArt);
			
			logger.warn(MsgErr);
			
			throw new NotFoundException(MsgErr);
		}

		articoliService.DelArticolo(articolo);

		responseNode.put("code", HttpStatus.OK.toString());
		responseNode.put("message", "Eliminazione Articolo " + CodArt + " Eseguita Con Successo");

		return new ResponseEntity<>(responseNode, headers, HttpStatus.OK);
	}
}
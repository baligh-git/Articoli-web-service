package it.baligh.webapp.service;

import java.util.List;

import org.springframework.data.domain.Pageable;

import it.baligh.webapp.entities.Articoli;

public interface ArticoliService
{
	public Iterable<Articoli> SelTutti();
	
	public List<Articoli> SelByDescrizione(String descrizione);
		
	public List<Articoli> SelByDescrizione(String descrizione, Pageable pageable);
	
	public Articoli SelByCodArt(String codArt);
	
	public void DelArticolo(Articoli articolo);
	
	public void InsArticolo(Articoli articolo);
	 
}
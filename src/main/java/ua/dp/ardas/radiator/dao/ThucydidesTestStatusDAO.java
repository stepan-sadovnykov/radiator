package ua.dp.ardas.radiator.dao;

import java.util.List;

import ua.dp.ardas.radiator.jobs.thucydides.test.result.ThucydidesTestStatistic;

public interface ThucydidesTestStatusDAO {
	
	void insert(ThucydidesTestStatistic state);
	List<ThucydidesTestStatistic> findAll();

}
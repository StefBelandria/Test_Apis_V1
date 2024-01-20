package com.codigo.msexamenexp.service.impl;

import com.codigo.msexamenexp.aggregates.request.RequestEnterprises;
import com.codigo.msexamenexp.aggregates.response.ResponseBase;
import com.codigo.msexamenexp.aggregates.constants.Constants;
import com.codigo.msexamenexp.aggregates.response.ResponseSunat;
import com.codigo.msexamenexp.config.RedisService;
import com.codigo.msexamenexp.entity.DocumentsTypeEntity;
import com.codigo.msexamenexp.entity.EnterprisesEntity;
import com.codigo.msexamenexp.entity.EnterprisesTypeEntity;
import com.codigo.msexamenexp.feignClient.SunatClient;
import com.codigo.msexamenexp.repository.DocumentsTypeRepository;
import com.codigo.msexamenexp.repository.EnterprisesRepository;
import com.codigo.msexamenexp.service.EnterprisesService;
import com.codigo.msexamenexp.util.EnterprisesValidations;
import com.codigo.msexamenexp.util.Util;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.Optional;

@Service
public class EnterprisesServiceImpl implements EnterprisesService {

    private  final SunatClient sunatClient;


    private final EnterprisesRepository enterprisesRepository;
    private final EnterprisesValidations enterprisesValidations;
    private final DocumentsTypeRepository typeRepository;

    private final RedisService redisService;
    private final Util util;

    public EnterprisesServiceImpl(SunatClient sunatClient, EnterprisesRepository enterprisesRepository, EnterprisesValidations enterprisesValidations, DocumentsTypeRepository typeRepository, RedisService redisService, Util util) {
        this.sunatClient = sunatClient;
        this.enterprisesRepository = enterprisesRepository;
        this.enterprisesValidations = enterprisesValidations;
        this.typeRepository = typeRepository;
        this.redisService = redisService;
        this.util = util;
    }

    @Value("${token.ap1.sunat}")
    private String tokenSunat;

    @Value("${time.expiration.sunat.info")
    private String timeExperitionSunatInfo;


    @Override
    public ResponseBase createEnterprise(RequestEnterprises requestEnterprises) {
        boolean validate = enterprisesValidations.validateInput(requestEnterprises);
        if(validate){
            EnterprisesEntity enterprises = getEntity(requestEnterprises);
            enterprisesRepository.save(enterprises);
            return new ResponseBase(Constants.CODE_SUCCESS,Constants.MESS_SUCCESS, Optional.of(enterprises));
        }else{
            return new ResponseBase(Constants.CODE_ERROR_DATA_INPUT,Constants.MESS_ERROR_DATA_NOT_VALID,null);
        }
    }

    @Override
    public ResponseBase findOneEnterprise(String doc) {
        EnterprisesEntity enterprisesEntity = enterprisesRepository.findByNumDocument(doc);
        return new ResponseBase(Constants.CODE_SUCCESS,Constants.MESS_SUCCESS, Optional.of(enterprisesEntity));

    }

    @Override
    public ResponseBase findAllEnterprises() {
        Optional allEnterprises = Optional.of(enterprisesRepository.findAll());
        if(allEnterprises.isPresent()){
            return new ResponseBase(Constants.CODE_SUCCESS,Constants.MESS_SUCCESS,allEnterprises);
        }
        return new ResponseBase(Constants.CODE_ERROR_DATA_NOT,Constants.MESS_ZERO_ROWS,Optional.empty());
    }

    @Override
    public ResponseBase updateEnterprise(Integer id, RequestEnterprises requestEnterprises) {
            Optional<EnterprisesEntity> enterprises = enterprisesRepository.findById(id);
            boolean validationEntity = enterprisesValidations.validateInputUpdate(requestEnterprises);
            if(validationEntity){
                EnterprisesEntity enterprisesUpdate = getEntityUpdate(requestEnterprises,enterprises.get());
                enterprisesRepository.save(enterprisesUpdate);
                return new ResponseBase(Constants.CODE_SUCCESS,Constants.MESS_SUCCESS,Optional.of(enterprisesUpdate));
            }else {
                return new ResponseBase(Constants.CODE_ERROR_DATA_INPUT,Constants.MESS_ERROR_DATA_NOT_VALID,Optional.empty());
            }

    }

    @Override
    public ResponseBase delete(Integer id) {

    return null;
    }

    private EnterprisesEntity getEntity(RequestEnterprises requestEnterprises){
        EnterprisesEntity entity = new EnterprisesEntity();
        entity.setStatus(Constants.STATUS_ACTIVE);
        entity.setEnterprisesTypeEntity(getEnterprisesType(requestEnterprises));
        entity.setDocumentsTypeEntity(getDocumentsType(requestEnterprises));
        entity.setUserCreate(Constants.AUDIT_ADMIN);
        entity.setDateCreate(getTimestamp());

        return entity;
    }
    private EnterprisesEntity getEntityUpdate(RequestEnterprises requestEnterprises, EnterprisesEntity enterprisesEntity){
        enterprisesEntity.setNumDocument(requestEnterprises.getNumDocument());
        enterprisesEntity.setBusinessName(requestEnterprises.getBusinessName());
        enterprisesEntity.setEnterprisesTypeEntity(getEnterprisesType(requestEnterprises));
        enterprisesEntity.setUserModif(Constants.AUDIT_ADMIN);
        enterprisesEntity.setDateModif(getTimestamp());
        return enterprisesEntity;
    }
    private EnterprisesEntity getEntityDelete(EnterprisesEntity enterprisesEntity){

        return null;
    }

    private EnterprisesTypeEntity getEnterprisesType(RequestEnterprises requestEnterprises){
        EnterprisesTypeEntity typeEntity = new EnterprisesTypeEntity();

        typeEntity.setIdEnterprisesType(requestEnterprises.getEnterprisesTypeEntity());
        return typeEntity;
    }

    public ResponseSunat getExecutionSunat (String numero){
        String redisCache = redisService.getValueByKey(Constants.REDIS_KEY_INFO_SUNAT+numero);
        if (redisCache!= null ) {
            ResponseSunat sunat = util.convertFromJson(redisCache, ResponseSunat.class);
            return sunat;
            }else {
                String authorizathion = "Bearer" + tokenSunat;
                ResponseSunat sunat = sunatClient.getInfoSunat(numero,authorizathion);
                String redisData = util.convertToJsonEntity(sunat);
                redisService.saveKeyValue(Constants.REDIS_KEY_INFO_SUNAT+numero,redisData,Integer.valueOf(timeExperitionSunatInfo));
                return sunat;
        }
    }

    private DocumentsTypeEntity getDocumentsType(RequestEnterprises requestEnterprises){
        DocumentsTypeEntity typeEntity = typeRepository.findByCodType(Constants.COD_TYPE_RUC);
        return  typeEntity;
    }

    private Timestamp getTimestamp(){
        long currentTime = System.currentTimeMillis();
        Timestamp timestamp = new Timestamp(currentTime);
        return timestamp;
    }

}

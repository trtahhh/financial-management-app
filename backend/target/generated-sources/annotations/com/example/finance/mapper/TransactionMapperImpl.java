package com.example.finance.mapper;

import com.example.finance.dto.TransactionDTO;
import com.example.finance.entity.Category;
import com.example.finance.entity.Transaction;
import com.example.finance.entity.User;
import com.example.finance.entity.Wallet;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import javax.annotation.processing.Generated;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-08-01T02:48:51+0700",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.15 (Eclipse Adoptium)"
)
@Component
public class TransactionMapperImpl implements TransactionMapper {

    private final DatatypeFactory datatypeFactory;

    public TransactionMapperImpl() {
        try {
            datatypeFactory = DatatypeFactory.newInstance();
        }
        catch ( DatatypeConfigurationException ex ) {
            throw new RuntimeException( ex );
        }
    }

    @Override
    public TransactionDTO toDto(Transaction entity) {
        if ( entity == null ) {
            return null;
        }

        TransactionDTO transactionDTO = new TransactionDTO();

        transactionDTO.setUserId( entityUserId( entity ) );
        transactionDTO.setWalletId( entityWalletId( entity ) );
        transactionDTO.setCategoryId( entityCategoryId( entity ) );
        transactionDTO.setDate( entity.getDate() );
        transactionDTO.setFilePath( entity.getFilePath() );
        transactionDTO.setStatus( entity.getStatus() );
        transactionDTO.setTags( entity.getTags() );
        transactionDTO.setIsDeleted( entity.isDeleted() );
        transactionDTO.setDeletedAt( xmlGregorianCalendarToLocalDate( localDateTimeToXmlGregorianCalendar( entity.getDeletedAt() ) ) );
        transactionDTO.setCreatedAt( xmlGregorianCalendarToLocalDate( localDateTimeToXmlGregorianCalendar( entity.getCreatedAt() ) ) );
        transactionDTO.setUpdatedAt( xmlGregorianCalendarToLocalDate( localDateTimeToXmlGregorianCalendar( entity.getUpdatedAt() ) ) );
        transactionDTO.setId( entity.getId() );
        transactionDTO.setAmount( entity.getAmount() );
        transactionDTO.setType( entity.getType() );
        transactionDTO.setNote( entity.getNote() );

        return transactionDTO;
    }

    @Override
    public Transaction toEntity(TransactionDTO dto) {
        if ( dto == null ) {
            return null;
        }

        Transaction transaction = new Transaction();

        transaction.setId( dto.getId() );
        transaction.setAmount( dto.getAmount() );
        transaction.setType( dto.getType() );
        transaction.setNote( dto.getNote() );
        transaction.setDate( dto.getDate() );
        transaction.setFilePath( dto.getFilePath() );
        transaction.setStatus( dto.getStatus() );
        transaction.setTags( dto.getTags() );
        transaction.setDeletedAt( xmlGregorianCalendarToLocalDateTime( localDateToXmlGregorianCalendar( dto.getDeletedAt() ) ) );
        transaction.setCreatedAt( xmlGregorianCalendarToLocalDateTime( localDateToXmlGregorianCalendar( dto.getCreatedAt() ) ) );
        transaction.setUpdatedAt( xmlGregorianCalendarToLocalDateTime( localDateToXmlGregorianCalendar( dto.getUpdatedAt() ) ) );

        return transaction;
    }

    private XMLGregorianCalendar localDateToXmlGregorianCalendar( LocalDate localDate ) {
        if ( localDate == null ) {
            return null;
        }

        return datatypeFactory.newXMLGregorianCalendarDate(
            localDate.getYear(),
            localDate.getMonthValue(),
            localDate.getDayOfMonth(),
            DatatypeConstants.FIELD_UNDEFINED );
    }

    private XMLGregorianCalendar localDateTimeToXmlGregorianCalendar( LocalDateTime localDateTime ) {
        if ( localDateTime == null ) {
            return null;
        }

        return datatypeFactory.newXMLGregorianCalendar(
            localDateTime.getYear(),
            localDateTime.getMonthValue(),
            localDateTime.getDayOfMonth(),
            localDateTime.getHour(),
            localDateTime.getMinute(),
            localDateTime.getSecond(),
            localDateTime.get( ChronoField.MILLI_OF_SECOND ),
            DatatypeConstants.FIELD_UNDEFINED );
    }

    private static LocalDate xmlGregorianCalendarToLocalDate( XMLGregorianCalendar xcal ) {
        if ( xcal == null ) {
            return null;
        }

        return LocalDate.of( xcal.getYear(), xcal.getMonth(), xcal.getDay() );
    }

    private static LocalDateTime xmlGregorianCalendarToLocalDateTime( XMLGregorianCalendar xcal ) {
        if ( xcal == null ) {
            return null;
        }

        if ( xcal.getYear() != DatatypeConstants.FIELD_UNDEFINED
            && xcal.getMonth() != DatatypeConstants.FIELD_UNDEFINED
            && xcal.getDay() != DatatypeConstants.FIELD_UNDEFINED
            && xcal.getHour() != DatatypeConstants.FIELD_UNDEFINED
            && xcal.getMinute() != DatatypeConstants.FIELD_UNDEFINED
        ) {
            if ( xcal.getSecond() != DatatypeConstants.FIELD_UNDEFINED
                && xcal.getMillisecond() != DatatypeConstants.FIELD_UNDEFINED ) {
                return LocalDateTime.of(
                    xcal.getYear(),
                    xcal.getMonth(),
                    xcal.getDay(),
                    xcal.getHour(),
                    xcal.getMinute(),
                    xcal.getSecond(),
                    Duration.ofMillis( xcal.getMillisecond() ).getNano()
                );
            }
            else if ( xcal.getSecond() != DatatypeConstants.FIELD_UNDEFINED ) {
                return LocalDateTime.of(
                    xcal.getYear(),
                    xcal.getMonth(),
                    xcal.getDay(),
                    xcal.getHour(),
                    xcal.getMinute(),
                    xcal.getSecond()
                );
            }
            else {
                return LocalDateTime.of(
                    xcal.getYear(),
                    xcal.getMonth(),
                    xcal.getDay(),
                    xcal.getHour(),
                    xcal.getMinute()
                );
            }
        }
        return null;
    }

    private Long entityUserId(Transaction transaction) {
        if ( transaction == null ) {
            return null;
        }
        User user = transaction.getUser();
        if ( user == null ) {
            return null;
        }
        Long id = user.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private Long entityWalletId(Transaction transaction) {
        if ( transaction == null ) {
            return null;
        }
        Wallet wallet = transaction.getWallet();
        if ( wallet == null ) {
            return null;
        }
        Long id = wallet.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private Long entityCategoryId(Transaction transaction) {
        if ( transaction == null ) {
            return null;
        }
        Category category = transaction.getCategory();
        if ( category == null ) {
            return null;
        }
        Long id = category.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }
}

package com.example.finance.mapper;

import com.example.finance.dto.WalletDTO;
import com.example.finance.entity.User;
import com.example.finance.entity.Wallet;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-08-01T02:48:51+0700",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.15 (Eclipse Adoptium)"
)
@Component
public class WalletMapperImpl implements WalletMapper {

    @Override
    public WalletDTO toDto(Wallet entity) {
        if ( entity == null ) {
            return null;
        }

        WalletDTO walletDTO = new WalletDTO();

        walletDTO.setUserId( entityUserId( entity ) );
        walletDTO.setId( entity.getId() );
        walletDTO.setName( entity.getName() );
        walletDTO.setBalance( entity.getBalance() );

        return walletDTO;
    }

    @Override
    public Wallet toEntity(WalletDTO dto) {
        if ( dto == null ) {
            return null;
        }

        Wallet wallet = new Wallet();

        wallet.setUser( userFromId( dto.getUserId() ) );
        wallet.setId( dto.getId() );
        wallet.setName( dto.getName() );
        wallet.setBalance( dto.getBalance() );

        return wallet;
    }

    private Long entityUserId(Wallet wallet) {
        if ( wallet == null ) {
            return null;
        }
        User user = wallet.getUser();
        if ( user == null ) {
            return null;
        }
        Long id = user.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }
}

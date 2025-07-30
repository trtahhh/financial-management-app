package com.example.finance.mapper;

import com.example.finance.dto.WalletDTO;
import com.example.finance.entity.User;
import com.example.finance.entity.Wallet;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-07-30T19:03:33+0700",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.50.v20250628-1110, environment: Java 21.0.7 (Eclipse Adoptium)"
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
        walletDTO.setBalance( entity.getBalance() );
        walletDTO.setId( entity.getId() );
        walletDTO.setName( entity.getName() );

        return walletDTO;
    }

    @Override
    public Wallet toEntity(WalletDTO dto) {
        if ( dto == null ) {
            return null;
        }

        Wallet wallet = new Wallet();

        wallet.setUser( userFromId( dto.getUserId() ) );
        wallet.setBalance( dto.getBalance() );
        wallet.setId( dto.getId() );
        wallet.setName( dto.getName() );

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

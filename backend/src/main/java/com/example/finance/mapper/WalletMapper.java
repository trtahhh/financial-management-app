package com.example.finance.mapper;

import com.example.finance.dto.WalletDTO;
import com.example.finance.entity.Wallet;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import com.example.finance.entity.User;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface WalletMapper {
    @Mapping(target = "userId", source = "user.id")
    WalletDTO toDto(Wallet entity);

    @Mapping(target = "user", source = "userId", qualifiedByName = "userFromId")
    Wallet toEntity(WalletDTO dto);

    @Named("userFromId")
    default User userFromId(Long id) {
        if (id == null) return null;
        User u = new User();
        u.setId(id);
        return u;
    }
}

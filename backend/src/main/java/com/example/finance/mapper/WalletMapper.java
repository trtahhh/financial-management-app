package com.example.finance.mapper;

import com.example.finance.dto.WalletDTO;
import com.example.finance.entity.Wallet;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import com.example.finance.entity.User;
import com.example.finance.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class WalletMapper {
    
    @Autowired
    protected UserRepository userRepository;
    
    @Mapping(target = "userId", source = "user.id")
    public abstract WalletDTO toDto(Wallet entity);

    @Mapping(target = "user", source = "userId", qualifiedByName = "userFromId")
    public abstract Wallet toEntity(WalletDTO dto);

    @Named("userFromId")
    protected User userFromId(Long id) {
        if (id == null) return null;
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }
}

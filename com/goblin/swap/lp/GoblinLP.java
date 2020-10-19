package com.goblin.swap.lp;

import io.nuls.contract.sdk.Address;
import io.nuls.contract.sdk.Contract;
import io.nuls.contract.sdk.Msg;
import io.nuls.contract.sdk.annotation.JSONSerializable;
import io.nuls.contract.sdk.annotation.Required;
import io.nuls.contract.sdk.annotation.View;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import static io.nuls.contract.sdk.Utils.emit;
import static io.nuls.contract.sdk.Utils.require;

public class GoblinLP extends Ownable implements Contract {

    private Map<Address, BigInteger> lpTotalMap = new HashMap<Address, BigInteger>();
    private Map<Address, BigInteger> valueMap = new HashMap<Address, BigInteger>();
    private Map<Address, BigInteger> tokenMap = new HashMap<Address, BigInteger>();
    private Map<Address, BigInteger> lpLockedMap = new HashMap<Address, BigInteger>();

    private Map<Address, Boolean> allowedContracts = new HashMap<Address, Boolean>();


    public void addAllowedContract(Address address) {
        onlyOwner();
        require(address != null, "address can't be null");
        allowedContracts.put(address, true);
    }

    public void removeAllowedContract(Address address) {
        onlyOwner();
        require(address != null, "address can't be null");
        allowedContracts.put(address, false);
    }

    public void addLiquidity(Address account, BigInteger lpAmount, BigInteger value, BigInteger tokenAmount) {
        require(Msg.sender().equals(owner) || (allowedContracts.get(Msg.sender()) != null && allowedContracts.get(Msg.sender())),"only allow contract can call this method");
        require(lpAmount.compareTo(BigInteger.ZERO) > 0 && value.compareTo(BigInteger.ZERO) > 0 && tokenAmount.compareTo(BigInteger.ZERO) > 0,"add amount must be >0");

        if (lpTotalMap.get(account) != null) {
            lpTotalMap.put(account, lpTotalMap.get(account).add(lpAmount));
        } else {
            lpTotalMap.put(account, lpAmount);
        }
        if (tokenMap.get(account) != null) {
            tokenMap.put(account, tokenMap.get(account).add(tokenAmount));
        } else {
            tokenMap.put(account, tokenAmount);
        }
        if (valueMap.get(account) != null) {
            valueMap.put(account, valueMap.get(account).add(value));
        } else {
            valueMap.put(account, value);
        }

    }

    public void lockLiquidity(Address account, BigInteger lpAmount) {
        require(lpAmount.compareTo(BigInteger.ZERO) > 0,"lockLiquidity amount must be >0");
        require(Msg.sender().equals(owner) || (allowedContracts.get(Msg.sender()) != null && allowedContracts.get(Msg.sender())),"only allow contract can call this method");
        BigInteger canUsed = getCanUsedLpAmount(account);
        require(canUsed.compareTo(lpAmount) >= 0, "lp is not enough");
        if (lpLockedMap.get(account) != null) {
            lpLockedMap.put(account, lpLockedMap.get(account).add(lpAmount));
        } else {
            lpLockedMap.put(account, lpAmount);
        }

    }

    public void withdrawLiquidity(Address account, BigInteger lpAmount) {
        require(lpAmount.compareTo(BigInteger.ZERO) > 0,"withdrawLiquidity amount must be >0");
        require(Msg.sender().equals(owner) || (allowedContracts.get(Msg.sender()) != null && allowedContracts.get(Msg.sender())),"only allow contract can call this method");
        BigInteger lockLp = getLockedLpAmount(account);
        require(lockLp.compareTo(lpAmount) >= 0);

        lpLockedMap.put(account, lockLp.subtract(lpAmount));
    }

    public void removeLiquidity(Address account, BigInteger lpAmount, BigInteger value, BigInteger tokenAmount) {
        require(lpAmount.compareTo(BigInteger.ZERO) > 0 && value.compareTo(BigInteger.ZERO) > 0 && tokenAmount.compareTo(BigInteger.ZERO) > 0,"remove amount must be >0");
        require(Msg.sender().equals(owner) || (allowedContracts.get(Msg.sender()) != null && allowedContracts.get(Msg.sender())),"only allow contract can call this method");
        BigInteger canUsed = getCanUsedLpAmount(account);
        require(canUsed.compareTo(lpAmount) >= 0, "lp is not enough");
        lpTotalMap.put(account, lpTotalMap.get(account).subtract(lpAmount));
        valueMap.put(account, valueMap.get(account).subtract(value));
        tokenMap.put(account, tokenMap.get(account).subtract(tokenAmount));
    }


    @View
    public BigInteger getCanUsedLpAmount(Address account) {
        if (lpTotalMap.get(account) != null) {
            BigInteger lockedAmount = lpLockedMap.get(account) != null ? lpLockedMap.get(account) : BigInteger.ZERO;
            BigInteger unUsedAmount = lpTotalMap.get(account).subtract(lockedAmount);
            return unUsedAmount;
        } else {
            return BigInteger.ZERO;
        }
    }

    @View
    public BigInteger getLockedLpAmount(Address account) {
        return lpLockedMap.get(account) == null ? BigInteger.ZERO : lpLockedMap.get(account);
    }

    @View
    public BigInteger getAccountValue(Address account) {
        return valueMap.get(account) == null ? BigInteger.ZERO : valueMap.get(account);
    }

    @View
    public BigInteger getAccountTokenAmount(Address account) {
        return tokenMap.get(account) == null ? BigInteger.ZERO : tokenMap.get(account);
    }

    @View
    public BigInteger getLpAmount(Address account) {
        return lpTotalMap.get(account) == null ? BigInteger.ZERO : lpTotalMap.get(account);
    }

    @JSONSerializable
    @View
    public BigInteger[] getAccountInfo(Address account) {
        BigInteger lp = getLpAmount(account);
        BigInteger value = getAccountValue(account);
        BigInteger token = getAccountTokenAmount(account);
        return new BigInteger[]{lp, value, token};

    }


}

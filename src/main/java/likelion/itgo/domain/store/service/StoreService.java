package likelion.itgo.domain.store.service;

import likelion.itgo.domain.member.entity.Member;
import likelion.itgo.domain.member.repository.MemberRepository;
import likelion.itgo.domain.store.dto.StoreInfoResponse;
import likelion.itgo.domain.store.dto.StoreRegisterRequest;
import likelion.itgo.domain.store.dto.StoreUpdateRequest;
import likelion.itgo.domain.store.entity.Store;
import likelion.itgo.domain.store.repository.StoreRepository;
import likelion.itgo.global.error.GlobalErrorCode;
import likelion.itgo.global.error.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StoreService {

    private final StoreRepository storeRepository;
    private final MemberRepository memberRepository;

    /**
     * 가게 정보 등록
     */
    @Transactional
    public StoreInfoResponse registerStore(Long memberId, StoreRegisterRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.NOT_FOUND, "해당 ID의 회원을 찾을 수 없습니다: " + memberId));

        // 연관관계 설정 + DB 추가
        Store store = request.toEntity();
        member.registerStore(store);
        storeRepository.save(store);

        return StoreInfoResponse.of(store);
    }

    /**
     * 가게 정보 조회
     */
    public StoreInfoResponse getStoreByMemberId(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.NOT_FOUND, "해당 ID의 회원을 찾을 수 없습니다: " + memberId));

        Store store = member.getStore();
        if (store == null) {
            throw new CustomException(GlobalErrorCode.NOT_FOUND, "해당 회원은 등록된 가게가 없습니다.");
        }
        return StoreInfoResponse.of(store);
    }

    /**
     * 가게 정보 수정
     */
    @Transactional
    public StoreInfoResponse updateStore(Long memberId, StoreUpdateRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.NOT_FOUND, "해당 ID의 회원을 찾을 수 없습니다: " + memberId));

        Store store = member.getStore();
        if (store == null) {
            throw new CustomException(GlobalErrorCode.NOT_FOUND, "등록된 가게가 없습니다.");
        }

        // Store 엔티티에서 직접 업데이트
        store.update(request);
        return StoreInfoResponse.of(store);
    }

    /**
     * 가게 정보 삭제
     */
    @Transactional
    public void deleteStore(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.NOT_FOUND, "해당 ID의 회원을 찾을 수 없습니다: " + memberId));

        Store store = member.getStore();
        if (store == null) {
            throw new CustomException(GlobalErrorCode.NOT_FOUND, "삭제할 가게가 존재하지 않습니다.");
        }

        // 연관관계 끊기 + DB 삭제
        member.removeStore();
        storeRepository.delete(store);
    }

}

package 
com.unilus.careconnect.repository;
import 
com.unilus.careconnect.entity.CounsellorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface CounsellorProfileRepository
extends 
JpaRepository<CounsellorProfile, Long> {
Optional<CounsellorProfile> findByUserId(Long userId);

boolean existsByUserId(Long userId);
}

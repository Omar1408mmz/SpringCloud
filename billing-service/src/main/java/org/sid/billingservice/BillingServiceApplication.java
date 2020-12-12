package org.sid.billingservice;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.*;
import javax.swing.text.BadLocationException;
import java.awt.dnd.DropTarget;
import java.util.Collection;
import java.util.Date;


@Entity
@Data @AllArgsConstructor @NoArgsConstructor
class Bill {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private  Long id;
	private Date billingDate;
	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	private Long customerID;
	@Transient private Customer customer;
	@OneToMany(mappedBy = "bill")
	private Collection<ProductItem> productItems;

}

@RepositoryRestResource
interface BillRepository extends JpaRepository<Bill,Long>{

}

@Projection(name = "fullBill",types = Bill.class)
interface BillProjection{
	public Long getId();
	public Date getBillingDate();
	public Long getCustomerID();
	public Collection<ProductItem> getProductItems();
}

@Entity
@Data @AllArgsConstructor @NoArgsConstructor
class ProductItem{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	private Long productID;
	@Transient private Product product;
	private double price;
	private double quantity;
	@ManyToOne
	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	private Bill bill;
}

@RepositoryRestResource
interface ProductItemRepository extends JpaRepository<ProductItem,Long>{

}

@Data
class Customer{
	private Long id ;
	private String  name ;
	private String email ;

}

@FeignClient(name = "CUSTOMER-SERVICE")
interface CustomerService{
	@GetMapping("/customers/{id}")
	public Customer findCustomerById(@PathVariable(name="id") Long id);

}

@Data
class Product{
	private Long id ;
	private String nom ;
	private double price ;
}

@FeignClient(name = "INVENTORY-SERVICE")
interface ProductService{
	@GetMapping("/products/{id}")
	public Product findProductById(@PathVariable(name ="id") Long id);
    @GetMapping("/products")
	public PagedModel<Product> findAllProducts();
}

@SpringBootApplication
@EnableFeignClients
public class BillingServiceApplication {

	public static void main(String[] args) {

		SpringApplication.run(BillingServiceApplication.class, args);
	}


	@Bean
	CommandLineRunner start(BillRepository billRepository,
							ProductItemRepository productItemRepository,
							CustomerService customerservice,
							ProductService productservice
							){
		return args -> {
			Customer c1 = customerservice.findCustomerById(1L);
			Bill bill1 = billRepository.save(new Bill(null,new Date(),c1.getId(),null,null));

			PagedModel<Product> products = productservice.findAllProducts();
			products.getContent().forEach(p->{
				productItemRepository.save(new ProductItem(null,p.getId(),null,p.getPrice(),30,bill1));
			});


		};

	}
	@RestController
	 class BillRestController{
		@Autowired
		private BillRepository billRepository;
		@Autowired
		private ProductItemRepository productItemRepository;
		@Autowired
		private CustomerService customerService;
		@Autowired
		private ProductService productService;

		@GetMapping("/fullBill/{id}")
		public Bill getBill(@PathVariable(name ="id") Long id){
			Bill bill = billRepository.findById(id).get();
			bill.setCustomer(customerService.findCustomerById(bill.getCustomerID()));
			bill.getProductItems().forEach(pi->{
				pi.setProduct(productService.findProductById(pi.getProductID()));

					}
			);

			return bill;

		}


	 }

}

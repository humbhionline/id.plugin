# HumBhiOnline - Open Network
# Functional Components that make up the network
1. The network participants 
	* Seller 
	* Seller App (Bpp)
	* Buyer App (Bap)
	* Buyer 

	
	


1. id platform where any user of the network ( a bpp admin or bap admin or network admin )
	* register themselves
	* register emails and validate them
	* register phones and validate them
	* complete self kyc 
		* aadhar offline ekyc
		* absorb verified address.

	- Domain Administration
		* register domains they control
		* verify domains under their control
			*  TXT Record
	* Authorized Applications.
		* application_id / Secret generation for basic auth
		* Regeneration of secret. 
		* Register application's 
			* ed25519 public key to be used for validating signatures by applicatons 
			* x25519 public key  to be used for encrypting signatures from those applications
		* whitelisted ip addresses from where it would make calls to the id platform
	* Beckn Registration
		* subscriber_id
		* Type BAP|BPP 
		* domain
		* subscriber_url  
		
	 
1. key tool platform
	* uses id platform to login
	* uses hashicorp key vaults
	* provides apis 
			That take auth_token for authentication of the caller received from id platform
		
			/generate_key_pair/:algo
				uses hashicorp to generate id'ed key-pairs and storing in vault
			/public_key/:key_id 
				get public key to be shared with others. 
					
			/verify 
				HEADER
					the authorizaton header
				POST
					{
						"public_key" : key 
						"signature" : signature
						"payload" : payload
					}
				RESPONSE 
					{
						"verified" : Y|N
					}	
			/sign
				HEADER
					the authorizaton header
				
				POST 
					{ 
						"key_id" : key_id 
						"payload" : payload
					}
				RESPONSE 
					{
						"signature" : signature
					}
			/encrypt
				HEADER
					the authorizaton header
				POST 
					{ 
						"public_key" : key
						"key_id" : key_id 
						"payload" : payload
					}
				RESPONSE 
					{
						encrypted_payload  : encrypted_payload
					}
			/decrypt
				HEADER
					the authorizaton header
				POST 
					{ 
						"public_key" : key
						"key_id" : key_id 
						"encrypted_payload" : encrypted_payload
					}
				RESPONSE 
					{
						"payload" : payload 
					}
# Commercial model
	B -> S (COD/UPI..)
	S->Bpp ( Subscription. Buy order credits @15 Rs per order )
	Bpp->accelarator ( Subscription . buy order credits @2Rs per order)  
	
	Bpp->N (Subscription to lookup services for bandwidth consumption) - 1 
	Bap->N ( Subscription to lookup services for  bandwidth consumption) - 1 
	
	Bpp->Bap 5 Rs per order (finder fees).
	Bap -> BG/Registry local mirrors Tsp ( Bandwidth saver products ) - 2 
	
# Model 2
	B->Bap X 
	Bap->accelarator (6)
	Bap->Bap  3.0%
	Bap->N    0.2%
	Bap->Bpp  5.0%
	Bpp->accelarator (2)
	Bap->S   (100- 10 - 2.5 -1 )%
